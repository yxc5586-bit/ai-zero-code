package com.cyx.aizerocode.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.config.ZeroCodeProperties;
import com.cyx.aizerocode.constant.AppConstant;
import com.cyx.aizerocode.langgraph4j.tools.SpringContextUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 项目文件沙箱，所有 AI 文件工具都必须先经过这里解析路径。
 */
final class ProjectFileSecurityUtil {

    static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    private static final Set<String> WRITABLE_EXTENSIONS = Set.of(
            ".vue", ".ts", ".js", ".json", ".css", ".html", ".svg", ".md", ".txt"
    );

    private static final Set<String> WRITABLE_FILENAMES = Set.of(
            ".gitignore", ".editorconfig", ".prettierrc", ".npmrc"
    );

    private static final Set<String> PROTECTED_FILENAMES = Set.of(
            "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "vite.config.js", "vite.config.ts", "vue.config.js",
            "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
            "index.html", "main.js", "main.ts", "app.vue", ".gitignore", "readme.md"
    );

    private static final long MAX_WRITE_BYTES = 512 * 1024;

    private ProjectFileSecurityUtil() {
    }

    static Path resolveExistingFile(String relativeFilePath, Long appId) throws IOException {
        return resolveExisting(relativeFilePath, appId, "文件", Files::isRegularFile);
    }

    static Path resolveExistingDirectory(String relativeDirPath, Long appId) throws IOException {
        return resolveExisting(relativeDirPath, appId, "目录", Files::isDirectory);
    }

    static Path resolveWritableFile(String relativeFilePath, Long appId, String content) throws IOException {
        return resolveWritable(relativeFilePath, appId, content).path();
    }

    static Path resolveModifiableFile(String relativeFilePath, Long appId, String content) throws IOException {
        ResolvedPath resolvedPath = resolveWritable(relativeFilePath, appId, content);
        requireExistingType(resolvedPath.path(), Files.isRegularFile(resolvedPath.path()), "文件");
        return requireRealPathInProject(resolvedPath);
    }

    static Path resolveDeletableFile(String relativeFilePath, Long appId) throws IOException {
        Path path = resolveExistingFile(relativeFilePath, appId);
        if (isProtectedFile(path.getFileName().toString())) {
            throw new IllegalArgumentException("不允许删除重要文件 - " + path.getFileName());
        }
        return path;
    }

    static void checkWriteContentSize(String relativeFilePath, String content) {
        requireWriteSize(relativeFilePath, content);
    }

    static boolean shouldIgnore(Path relativePath) {
        for (Path part : relativePath) {
            if (shouldIgnore(part.toString())) {
                return true;
            }
        }
        return false;
    }

    static boolean shouldIgnore(String fileName) {
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return lowerName.startsWith(".env.")
                || IGNORED_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
    }

    static Path getProjectRoot(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException("应用 ID 无效");
        }
        return Paths.get(getCodeOutputRoot(), "vue_project_" + appId)
                .toAbsolutePath()
                .normalize();
    }

    private static String getCodeOutputRoot() {
        try {
            return SpringContextUtil.getBean(ZeroCodeProperties.class).getCode().getOutputRoot();
        } catch (Exception ignored) {
            return AppConstant.CODE_OUTPUT_ROOT_DIR;
        }
    }

    private static ResolvedPath resolve(String relativePath, Long appId) {
        Path projectRoot = getProjectRoot(appId);
        String input = normalizeInput(relativePath);
        Path userPath = Paths.get(input).normalize();
        if (userPath.isAbsolute() || userPath.getRoot() != null) {
            throw new IllegalArgumentException("只允许使用项目内相对路径 - " + relativePath);
        }
        if (startsWithParentTraversal(userPath)) {
            throw new IllegalArgumentException("路径不能越过项目目录 - " + relativePath);
        }

        Path resolvedPath = projectRoot.resolve(userPath).normalize();
        if (!resolvedPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("路径不能越过项目目录 - " + relativePath);
        }
        return new ResolvedPath(projectRoot, resolvedPath, relativePath);
    }

    private static Path resolveExisting(
            String relativePath,
            Long appId,
            String targetType,
            Predicate<Path> typeCheck
    ) throws IOException {
        ResolvedPath resolvedPath = resolve(relativePath, appId);
        rejectIgnored(resolvedPath, targetType);
        requireExistingType(resolvedPath.path(), typeCheck.test(resolvedPath.path()), targetType);
        return requireRealPathInProject(resolvedPath);
    }

    private static ResolvedPath resolveWritable(String relativeFilePath, Long appId, String content) throws IOException {
        ResolvedPath resolvedPath = resolve(relativeFilePath, appId);
        rejectIgnored(resolvedPath, "文件");
        requireWritableName(resolvedPath);
        requireWriteSize(relativeFilePath, content);
        requireWritableLocation(resolvedPath);
        return resolvedPath;
    }

    private static String normalizeInput(String relativePath) {
        String input = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        if (input.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("路径包含非法字符");
        }
        if (hasWindowsDrivePrefix(input)) {
            throw new IllegalArgumentException("只允许使用项目内相对路径 - " + relativePath);
        }
        return input;
    }

    private static void rejectIgnored(ResolvedPath resolvedPath, String targetType) {
        if (shouldIgnore(resolvedPath.relativePath())) {
            throw new IllegalArgumentException("不允许访问被忽略的" + targetType + " - " + resolvedPath.originalPath());
        }
    }

    private static void requireExistingType(Path path, boolean expectedType, String targetType) {
        if (!Files.exists(path) || !expectedType) {
            throw new IllegalArgumentException(targetType + "不存在或不是" + targetType + " - " + path.getFileName());
        }
    }

    private static Path requireRealPathInProject(ResolvedPath resolvedPath) throws IOException {
        Path realRoot = realProjectRoot(resolvedPath.projectRoot());
        Path realPath = resolvedPath.path().toRealPath();
        if (!realPath.startsWith(realRoot)) {
            throw new IllegalArgumentException("路径不能越过项目目录 - " + resolvedPath.originalPath());
        }
        return realPath;
    }

    private static void requireWritableName(ResolvedPath resolvedPath) {
        String fileName = fileName(resolvedPath.path());
        if (WRITABLE_FILENAMES.contains(fileName)) {
            return;
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        boolean allowed = WRITABLE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        if (!allowed) {
            throw new IllegalArgumentException("不允许写入该类型文件 - " + resolvedPath.originalPath());
        }
    }

    private static void requireWriteSize(String relativeFilePath, String content) {
        if (content != null && content.getBytes(StandardCharsets.UTF_8).length > MAX_WRITE_BYTES) {
            throw new IllegalArgumentException("文件内容超过大小限制 - " + relativeFilePath);
        }
    }

    private static void requireWritableLocation(ResolvedPath resolvedPath) throws IOException {
        Files.createDirectories(resolvedPath.projectRoot());
        Path realRoot = realProjectRoot(resolvedPath.projectRoot());
        if (Files.exists(resolvedPath.path())) {
            if (!Files.isRegularFile(resolvedPath.path())) {
                throw new IllegalArgumentException("文件不存在或不是文件 - " + resolvedPath.originalPath());
            }
            requireRealPathInProject(resolvedPath);
            return;
        }

        Path existingParent = nearestExistingParent(resolvedPath.path());
        if (!existingParent.toRealPath().startsWith(realRoot)) {
            throw new IllegalArgumentException("路径不能越过项目目录 - " + resolvedPath.originalPath());
        }
    }

    private static Path realProjectRoot(Path projectRoot) throws IOException {
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("项目目录不存在 - " + projectRoot.getFileName());
        }
        return projectRoot.toRealPath();
    }

    private static Path nearestExistingParent(Path path) {
        Path parent = path.getParent();
        while (parent != null && !Files.exists(parent)) {
            parent = parent.getParent();
        }
        if (parent == null) {
            throw new IllegalArgumentException("无法定位文件父目录");
        }
        return parent;
    }

    private static Path relativePath(ResolvedPath resolvedPath) {
        return resolvedPath.projectRoot().relativize(resolvedPath.path());
    }

    private static String fileName(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("文件必须位于项目目录内");
        }
        return fileName.toString();
    }

    private static boolean isProtectedFile(String fileName) {
        return PROTECTED_FILENAMES.contains(fileName.toLowerCase(Locale.ROOT));
    }

    private static boolean startsWithParentTraversal(Path path) {
        return path.getNameCount() > 0 && StrUtil.equals(path.getName(0).toString(), "..");
    }

    private static boolean hasWindowsDrivePrefix(String path) {
        return path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':';
    }

    private record ResolvedPath(Path projectRoot, Path path, String originalPath) {

        private Path relativePath() {
            return ProjectFileSecurityUtil.relativePath(this);
        }
    }
}
