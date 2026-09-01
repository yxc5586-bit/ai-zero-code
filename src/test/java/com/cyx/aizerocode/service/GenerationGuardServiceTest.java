package com.cyx.aizerocode.service;

import com.cyx.aizerocode.ai.AiCodeGeneratorService;
import com.cyx.aizerocode.ai.AiCodeGeneratorServiceFactory;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.config.ZeroCodeProperties;
import com.cyx.aizerocode.controller.AppController;
import com.cyx.aizerocode.core.AiCodeGeneratorFacade;
import com.cyx.aizerocode.core.builder.VueProjectBuilder;
import com.cyx.aizerocode.core.parser.CodeParserExecutor;
import com.cyx.aizerocode.core.saver.CodeFileSaverExecutor;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.langgraph4j.tools.SpringContextUtil;
import com.cyx.aizerocode.model.entity.App;
import com.cyx.aizerocode.model.entity.User;
import com.cyx.aizerocode.service.impl.AppServiceImpl;
import com.cyx.aizerocode.utils.WebScreenshotUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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

/**
 * 第 4 天生产运行准备的公开离线回归测试。
 *
 * <p>仓库会忽略其他本地实验测试，因此该类与现有公开测试放在同一已跟踪源码文件中。</p>
 */
class ProductionRuntimePreparationTest {

    @TempDir
    Path tempDir;

    @Test
    void deploymentUrlUsesPublicSiteRoot() throws Exception {
        Path outputRoot = tempDir.resolve("output");
        Path sourceDir = outputRoot.resolve("html_1");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("index.html"), "ok");

        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getCode().setOutputRoot(outputRoot.toString());
        properties.getCode().setDeployRoot(tempDir.resolve("deploy").toString());
        properties.getCode().setPublicBaseUrl("http://host:8080/");

        App app = App.builder()
                .id(1L)
                .userId(2L)
                .codeGenType(CodeGenTypeEnum.HTML.getValue())
                .deployKey("key123")
                .build();
        User user = new User();
        user.setId(2L);

        AppServiceImpl service = spy(new AppServiceImpl());
        ReflectionTestUtils.setField(service, "zeroCodeProperties", properties);
        doReturn(app).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(App.class));
        doNothing().when(service).generateAppScreenshotAsync(1L, "http://host:8080/deploy/key123/");

        String deployUrl = service.deployApp(1L, user);

        assertEquals("http://host:8080/deploy/key123/", deployUrl);
        app.setUserId(null);
        assertEquals(deployUrl, service.getAppVO(app).getDeployUrl());
        assertTrue(service.getAppVO(app).getDeploymentAvailable());
        assertTrue(Files.isRegularFile(tempDir.resolve("deploy/key123/index.html")));
    }

    @Test
    void deploymentStatusRequiresExistingIndexFile() throws Exception {
        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getCode().setOutputRoot(tempDir.resolve("output").toString());
        properties.getCode().setDeployRoot(tempDir.resolve("deploy").toString());
        properties.getCode().setPublicBaseUrl("http://host:8080");

        AppServiceImpl service = new AppServiceImpl();
        ReflectionTestUtils.setField(service, "zeroCodeProperties", properties);
        App app = App.builder().deployKey("missing").build();

        assertFalse(service.getAppVO(app).getDeploymentAvailable());
        assertNull(service.getAppVO(app).getDeployUrl());
        app.setDeployKey(null);
        assertFalse(service.getAppVO(app).getDeploymentAvailable());
    }

    @Test
    void cleanupKeepsDeploymentsAndTargetsOnlyTopLevelArtifacts() throws Exception {
        String script = Files.readString(Path.of("deploy/scripts/cleanup-zerocode-artifacts.sh"));

        assertFalse(script.contains("tmp/code_deploy"));
        assertTrue(script.contains("-mindepth 1 -maxdepth 1"));
    }

    @Test
    void multiFileParserNormalizesStylesheetNameAndSaverRequiresAllFiles() throws Exception {
        String generatedCode = """
                ```html
                <link rel="stylesheet" href="./styles.css">
                <link rel="stylesheet" href="https://cdn.example.com/styles.css">
                <script src="script.js"></script>
                ```
                ```css
                body { color: #333; }
                ```
                ```javascript
                console.log('ok');
                ```
                """;
        MultiFileCodeResult result = (MultiFileCodeResult) CodeParserExecutor.parseCode(
                generatedCode,
                CodeGenTypeEnum.MULTI_FILE
        );

        assertTrue(result.getHtmlCode().contains("href=\"style.css\""));
        assertTrue(result.getHtmlCode().contains("https://cdn.example.com/styles.css"));
        File savedDir = CodeFileSaverExecutor.executorSaver(
                result,
                CodeGenTypeEnum.MULTI_FILE,
                8L,
                tempDir.toString()
        );
        assertTrue(new File(savedDir, "index.html").isFile());
        assertTrue(new File(savedDir, "style.css").isFile());
        assertTrue(new File(savedDir, "script.js").isFile());

        result.setCssCode(null);
        assertThrows(BusinessException.class, () -> CodeFileSaverExecutor.executorSaver(
                result, CodeGenTypeEnum.MULTI_FILE, 9L, tempDir.toString()
        ));
        result.setCssCode("body{}");
        result.setJsCode(null);
        assertThrows(BusinessException.class, () -> CodeFileSaverExecutor.executorSaver(
                result, CodeGenTypeEnum.MULTI_FILE, 10L, tempDir.toString()
        ));
    }

    @Test
    void streamingSaveFailurePropagatesToSubscriber() {
        AiCodeGeneratorServiceFactory factory = mock(AiCodeGeneratorServiceFactory.class);
        AiCodeGeneratorService aiService = mock(AiCodeGeneratorService.class);
        when(factory.getAiCodeGeneratorService(7L, CodeGenTypeEnum.MULTI_FILE)).thenReturn(aiService);
        when(aiService.generateMultiFileCodeStream("prompt")).thenReturn(Flux.just("""
                ```html
                <link rel="stylesheet" href="style.css"><script src="script.js"></script>
                ```
                ```javascript
                console.log('missing css');
                ```
                """));
        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getCode().setOutputRoot(tempDir.toString());
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        ReflectionTestUtils.setField(facade, "aiCodeGeneratorServiceFactory", factory);
        ReflectionTestUtils.setField(facade, "zeroCodeProperties", properties);

        assertThrows(BusinessException.class, () -> facade.generateAndSaveCodeStream(
                "prompt", CodeGenTypeEnum.MULTI_FILE, 7L
        ).collectList().block());
    }

    @Test
    void generationErrorReturnsBusinessErrorWithoutDoneEvent() {
        AppService appService = mock(AppService.class);
        UserService userService = mock(UserService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User();
        user.setId(2L);
        when(userService.getLoginUser(request)).thenReturn(user);
        when(appService.chatToGenCode(1L, "prompt", user)).thenReturn(
                Flux.error(new BusinessException(ErrorCode.OPERATION_ERROR, "保存失败"))
        );
        AppController controller = new AppController();
        ReflectionTestUtils.setField(controller, "appService", appService);
        ReflectionTestUtils.setField(controller, "userService", userService);

        List<ServerSentEvent<String>> events = controller.chatToGenCode(1L, "prompt", request)
                .collectList()
                .block();

        assertEquals(1, events.size());
        assertEquals("business-error", events.getFirst().event());
    }

    @Test
    void multiFileDeploymentRejectsMissingLocalAsset() throws Exception {
        Path outputRoot = tempDir.resolve("output");
        Path sourceDir = outputRoot.resolve("multi_file_3");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("style.css"), "body{}");
        Files.writeString(sourceDir.resolve("script.js"), "console.log('ok')");
        Files.writeString(sourceDir.resolve("index.html"),
                "<link href=\"style.css\"><script src=\"missing.js\"></script>");
        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getCode().setOutputRoot(outputRoot.toString());
        properties.getCode().setDeployRoot(tempDir.resolve("deploy").toString());
        properties.getCode().setPublicBaseUrl("http://host:8080");
        App app = App.builder()
                .id(3L)
                .userId(2L)
                .codeGenType(CodeGenTypeEnum.MULTI_FILE.getValue())
                .deployKey("multi3")
                .build();
        User user = new User();
        user.setId(2L);
        AppServiceImpl service = spy(new AppServiceImpl());
        ReflectionTestUtils.setField(service, "zeroCodeProperties", properties);
        doReturn(app).when(service).getById(3L);

        assertThrows(BusinessException.class, () -> service.deployApp(3L, user));

        Files.writeString(sourceDir.resolve("index.html"),
                "<link href=\"style.css\"><script src=\"script.js\"></script>");
        doReturn(true).when(service).updateById(any(App.class));
        doNothing().when(service).generateAppScreenshotAsync(3L, "http://host:8080/deploy/multi3/");
        assertEquals("http://host:8080/deploy/multi3/", service.deployApp(3L, user));
    }

    @Test
    void vueBuildUsesConfiguredNpmFlagsAndDeletesNodeModules() throws Exception {
        Path projectDir = tempDir.resolve("vue-project");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("package.json"), "{\"scripts\":{\"build\":\"fake\"}}");
        Path commandLog = projectDir.resolve("npm-arguments.txt");
        Path fakeNpm = createFakeNpm(projectDir, commandLog);
        Path cacheRoot = tempDir.resolve("npm-cache");

        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getBuild().setNpmExecutable(fakeNpm.toString());
        properties.getBuild().setNpmCacheRoot(cacheRoot.toString());

        boolean success = new VueProjectBuilder(properties).buildProject(projectDir.toString());

        assertTrue(success);
        assertTrue(Files.isRegularFile(projectDir.resolve("dist/index.html")));
        assertFalse(Files.exists(projectDir.resolve("node_modules")));
        List<String> commands = Files.readAllLines(commandLog);
        assertEquals(2, commands.size());
        assertTrue(commands.getFirst().contains("install"));
        assertTrue(commands.getFirst().contains("--no-audit"));
        assertTrue(commands.getFirst().contains("--no-fund"));
        assertTrue(commands.getFirst().contains("--prefer-offline"));
        assertTrue(commands.getFirst().contains("--cache"));
        assertTrue(commands.getFirst().contains(cacheRoot.toAbsolutePath().toString()));
        assertTrue(commands.get(1).contains("run build"));
    }

    @Test
    void disabledScreenshotDoesNotCreateChromeDriver() {
        ZeroCodeProperties properties = new ZeroCodeProperties();
        properties.getScreenshot().setEnabled(false);

        try (MockedStatic<SpringContextUtil> springContext = mockStatic(SpringContextUtil.class)) {
            springContext.when(() -> SpringContextUtil.getBean(ZeroCodeProperties.class)).thenReturn(properties);
            assertNull(WebScreenshotUtils.saveWebPageScreenshot("http://127.0.0.1"));
        }
    }

    @Test
    void screenshotFailureDoesNotUpdateCover() throws Exception {
        ScreenshotService screenshotService = mock(ScreenshotService.class);
        CountDownLatch screenshotCalled = new CountDownLatch(1);
        when(screenshotService.generateAndUploadScreenshot("http://host/deploy/key/"))
                .thenAnswer(invocation -> {
                    screenshotCalled.countDown();
                    throw new IllegalStateException("screenshot failed");
                });

        AppServiceImpl service = spy(new AppServiceImpl());
        ReflectionTestUtils.setField(service, "screenshotService", screenshotService);

        service.generateAppScreenshotAsync(1L, "http://host/deploy/key/");

        assertTrue(screenshotCalled.await(2, TimeUnit.SECONDS));
        verify(service, after(300).never()).updateById(any(App.class));
    }

    private Path createFakeNpm(Path projectDir, Path commandLog) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path script = projectDir.resolve(windows ? "fake-npm.cmd" : "fake-npm");
        String content;
        if (windows) {
            content = "@echo off\r\n"
                    + "echo %*>>\"" + commandLog + "\"\r\n"
                    + "if \"%1\"==\"install\" mkdir node_modules\r\n"
                    + "if \"%1\"==\"run\" (\r\n"
                    + "  mkdir dist\r\n"
                    + "  echo built>dist\\index.html\r\n"
                    + ")\r\n";
        } else {
            content = "#!/usr/bin/env sh\n"
                    + "printf '%s\\n' \"$*\" >> '" + commandLog + "'\n"
                    + "if [ \"$1\" = \"install\" ]; then mkdir -p node_modules; fi\n"
                    + "if [ \"$1\" = \"run\" ]; then mkdir -p dist; printf built > dist/index.html; fi\n";
        }
        Files.writeString(script, content);
        assertTrue(script.toFile().setExecutable(true) || windows);
        return script;
    }
}
