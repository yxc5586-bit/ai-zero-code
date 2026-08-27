package com.cyx.aizerocode.service;

import com.cyx.aizerocode.config.ZeroCodeProperties;
import com.cyx.aizerocode.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerationGuardServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> bucket;

    @Mock
    private RScript script;

    private ZeroCodeProperties properties;

    private GenerationGuardService generationGuardService;

    @BeforeEach
    void setUp() {
        properties = new ZeroCodeProperties();
        properties.getGeneration().setDemoMode(false);
        properties.getGeneration().setGlobalConcurrency(1);
        properties.getGeneration().setLeaseTtl(Duration.ofMinutes(10));
        generationGuardService = new GenerationGuardService(redissonClient, properties);
        when(redissonClient.<String>getBucket(anyString(), eq(StringCodec.INSTANCE))).thenReturn(bucket);
    }

    @Test
    void shouldRejectWhenAppAlreadyHasGeneration() {
        when(bucket.trySet(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> generationGuardService.acquire(10L, 20L)
        );

        assertEquals(42900, exception.getCode());
        verify(redissonClient, never()).getScript(StringCodec.INSTANCE);
    }

    @Test
    void shouldReleaseAppLeaseWhenGlobalSlotIsBusy() {
        when(bucket.trySet(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true, false);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                anyList(),
                any()
        )).thenReturn(1L);

        assertThrows(BusinessException.class, () -> generationGuardService.acquire(10L, 20L));

        verify(script, times(1)).eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                eq(List.of("ai:generation:lease:10")),
                any()
        );
    }

    @Test
    void shouldReleaseBothLeasesAfterGenerationFinishes() {
        when(bucket.trySet(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                anyList(),
                any()
        )).thenReturn(1L);

        GenerationGuardService.GenerationLease lease = generationGuardService.acquire(10L, 20L);
        generationGuardService.release(lease);

        verify(script, times(2)).eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                anyList(),
                any()
        );
    }

    @Test
    void shouldRejectExceededDailyQuotaAndReleaseBothLeases() {
        properties.getGeneration().setDemoMode(true);
        properties.getGeneration().setDailyQuota(10);
        when(bucket.trySet(anyString(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(redissonClient.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                anyList(),
                any()
        )).thenReturn(11L, 1L, 1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> generationGuardService.acquire(10L, 20L)
        );

        assertEquals(42900, exception.getCode());
        verify(script, times(3)).eval(
                eq(RScript.Mode.READ_WRITE),
                anyString(),
                eq(RScript.ReturnType.INTEGER),
                anyList(),
                any()
        );
    }
}
