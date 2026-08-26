package com.cyx.aizerocode.ai.tools;

import com.cyx.aizerocode.constant.AppConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不依赖 Spring、Redis 或真实大模型的文件工具安全测试。
 */
class ProjectFileSecurityUtilTest {

    private static final long APP_ID = 900001L;
    private static final long OTHER_APP_ID = 900002L;

    private final Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + APP_ID)
            .toAbsolutePath()
            .normalize();
    private final Path otherProjectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, "vue_project_" + OTHER_APP_ID)
            .toAbsolutePath()
            .normalize();

    @AfterEach
    void tearDown() throws IOException {
        deleteIfExists(projectRoot);
        deleteIfExists(otherProjectRoot);
    }

    @Test
    void shouldResolveNormalNestedPathInProject() throws IOException {
        Files.createDirectories(projectRoot);

        Path path = ProjectFileSecurityUtil.resolveWritableFile("src/components/Header.vue", APP_ID, "<template />");

        assertEquals(projectRoot.resolve("src/components/Header.vue").normalize(), path);
    }

    @Test
    void shouldRejectAbsolutePath() {
        Path absolutePath = Paths.get(System.getProperty("user.dir")).resolve("application.yml").toAbsolutePath();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile(absolutePath.toString(), APP_ID, "test")
        );

        assertTrue(exception.getMessage().contains("相对路径"));
    }

    @Test
    void shouldRejectWindowsDriveRelativePath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile("C:temp/App.vue", APP_ID, "test")
        );

        assertTrue(exception.getMessage().contains("相对路径"));
    }

    @Test
    void shouldRejectParentTraversal() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile("../../application.yml", APP_ID, "test")
        );

        assertTrue(exception.getMessage().contains("越过项目目录"));
    }

    @Test
    void shouldRejectCrossProjectAccess() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile("../vue_project_" + OTHER_APP_ID + "/src/App.vue", APP_ID, "test")
        );

        assertTrue(exception.getMessage().contains("越过项目目录"));
    }

    @Test
    void shouldRejectIgnoredFileNameAndExtension() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile(".env", APP_ID, "SECRET=1")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile(".env.local", APP_ID, "SECRET=1")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile("logs/app.log", APP_ID, "log")
        );
    }

    @Test
    void shouldRejectUnsupportedWriteExtension() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProjectFileSecurityUtil.resolveWritableFile("public/logo.png", APP_ID, "not a png")
        );

        assertTrue(exception.getMessage().contains("类型文件"));
    }

    @Test
    void shouldReadAndModifyOnlyProjectFiles() throws IOException {
        FileWriteTool writeTool = new FileWriteTool();
        FileReadTool readTool = new FileReadTool();
        FileModifyTool modifyTool = new FileModifyTool();

        String writeResult = writeTool.writeFile("src/App.vue", "<template>Hello</template>", APP_ID);
        String modifyResult = modifyTool.modifyFile("src/App.vue", "Hello", "Safe", APP_ID);
        String content = readTool.readFile("src/App.vue", APP_ID);

        assertTrue(writeResult.contains("文件写入成功"));
        assertTrue(modifyResult.contains("文件修改成功"));
        assertEquals("<template>Safe</template>", content);
    }

    @Test
    void shouldFilterIgnoredNamesAndExtensionsWhenReadingDirectory() throws IOException {
        FileDirReadTool readTool = new FileDirReadTool();
        Files.createDirectories(projectRoot.resolve("src"));
        Files.createDirectories(projectRoot.resolve("node_modules/pkg"));
        Files.writeString(projectRoot.resolve("src/App.vue"), "<template />", StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("app.log"), "log", StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("node_modules/pkg/index.js"), "console.log(1)", StandardCharsets.UTF_8);

        String structure = readTool.readDir("", APP_ID);

        assertTrue(structure.contains("App.vue"));
        assertFalse(structure.contains("app.log"));
        assertFalse(structure.contains("index.js"));
    }

    @Test
    void shouldRejectIgnoredDirectoryAsReadRoot() throws IOException {
        Files.createDirectories(projectRoot.resolve("node_modules/pkg"));
        Files.writeString(projectRoot.resolve("node_modules/pkg/index.js"), "console.log(1)", StandardCharsets.UTF_8);

        String structure = new FileDirReadTool().readDir("node_modules", APP_ID);

        assertTrue(structure.contains("不允许访问被忽略的目录"));
    }

    @Test
    void shouldDeleteNormalFileButKeepImportantFile() throws IOException {
        FileWriteTool writeTool = new FileWriteTool();
        FileDeleteTool deleteTool = new FileDeleteTool();

        writeTool.writeFile("src/temp.txt", "temp", APP_ID);
        writeTool.writeFile("src/App.vue", "<template />", APP_ID);

        String normalDeleteResult = deleteTool.deleteFile("src/temp.txt", APP_ID);
        String importantDeleteResult = deleteTool.deleteFile("src/App.vue", APP_ID);

        assertTrue(normalDeleteResult.contains("文件删除成功"));
        assertFalse(Files.exists(projectRoot.resolve("src/temp.txt")));
        assertTrue(importantDeleteResult.contains("不允许删除重要文件"));
        assertTrue(Files.exists(projectRoot.resolve("src/App.vue")));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldRejectSymlinkEscapeOnUnixLikeSystem() throws IOException {
        Files.createDirectories(projectRoot.resolve("src"));
        Path outsideFile = Files.createTempFile("zerocode-outside", ".vue");
        try {
            Files.writeString(outsideFile, "<template>outside</template>", StandardCharsets.UTF_8);
            Files.createSymbolicLink(projectRoot.resolve("src/escape.vue"), outsideFile);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ProjectFileSecurityUtil.resolveExistingFile("src/escape.vue", APP_ID)
            );

            assertTrue(exception.getMessage().contains("越过项目目录"));
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    private void deleteIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((first, second) -> second.compareTo(first))
                    .forEach(file -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
