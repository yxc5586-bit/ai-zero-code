package com.cyx.aizerocode.service;

import com.cyx.aizerocode.config.ZeroCodeProperties;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的防护，用于演示生成的并发和配额管理。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationGuardService {

    private static final String APP_LEASE_PREFIX = "ai:generation:lease:";

    private static final String GLOBAL_LEASE_PREFIX = "ai:generation:global:lease:";

    private static final String QUOTA_PREFIX = "ai:generation:quota:";

    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Shanghai");

    private static final String RELEASE_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """;

    private static final String INCREMENT_QUOTA_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIREAT', KEYS[1], ARGV[1])
            end
            return current
            """;

    private final RedissonClient redissonClient;

    private final ZeroCodeProperties zeroCodeProperties;

    public GenerationLease acquire(Long appId, Long userId) {
        ZeroCodeProperties.Generation generation = zeroCodeProperties.getGeneration();
        Duration leaseTtl = generation.getLeaseTtl();
        String appKey = APP_LEASE_PREFIX + appId;
        String appToken = UUID.randomUUID().toString();
        if (!tryAcquire(appKey, appToken, leaseTtl)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "当前应用正在生成中，请等待本次生成完成");
        }

        String globalKey = null;
        String globalToken = UUID.randomUUID().toString();
        try {
            int concurrency = Math.max(1, generation.getGlobalConcurrency());
            for (int slot = 0; slot < concurrency; slot++) {
                String candidateKey = GLOBAL_LEASE_PREFIX + slot;
                if (tryAcquire(candidateKey, globalToken, leaseTtl)) {
                    globalKey = candidateKey;
                    break;
                }
            }
            if (globalKey == null) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "当前生成任务已满，请稍后再试");
            }
            if (generation.isDemoMode()) {
                consumeDailyQuota(userId, generation.getDailyQuota());
            }
            return new GenerationLease(appKey, appToken, globalKey, globalToken);
        } catch (RuntimeException e) {
            releaseKey(globalKey, globalToken);
            releaseKey(appKey, appToken);
            throw e;
        }
    }

    public void release(GenerationLease lease) {
        if (lease == null) {
            return;
        }
        releaseKey(lease.globalKey(), lease.globalToken());
        releaseKey(lease.appKey(), lease.appToken());
    }

    private boolean tryAcquire(String key, String token, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        long ttlMillis = Math.max(1, ttl.toMillis());
        return bucket.trySet(token, ttlMillis, TimeUnit.MILLISECONDS);
    }

    private void consumeDailyQuota(Long userId, int dailyQuota) {
        if (dailyQuota <= 0) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "今日生成额度已用完");
        }
        ZonedDateTime now = ZonedDateTime.now(QUOTA_ZONE);
        long expiresAt = now.toLocalDate().plusDays(1).atStartOfDay(QUOTA_ZONE).toInstant().toEpochMilli();
        String quotaKey = QUOTA_PREFIX + now.toLocalDate().toString().replace("-", "") + ":" + userId;
        Number current = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                INCREMENT_QUOTA_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(quotaKey),
                expiresAt
        );
        if (current.longValue() > dailyQuota) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "今日生成次数已达上限，请明天再试");
        }
    }

    private void releaseKey(String key, String token) {
        if (key == null || token == null) {
            return;
        }
        try {
            redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    RELEASE_SCRIPT,
                    RScript.ReturnType.INTEGER,
                    List.of(key),
                    token
            );
        } catch (RuntimeException e) {
            log.warn("Failed to release generation lease, key={}", key, e);
        }
    }

    public record GenerationLease(String appKey, String appToken, String globalKey, String globalToken) {
    }
}
