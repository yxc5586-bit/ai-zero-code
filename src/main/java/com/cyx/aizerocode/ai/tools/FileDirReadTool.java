package com.cyx.aizerocode.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件目录读取工具
 * 使用 Hutool 简化文件操作
 */
@Component
@Slf4j
public class FileDirReadTool extends BaseTool{

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = ProjectFileSecurityUtil.resolveExistingDirectory(relativeDirPath, appId);
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            // 默认不跟随符号链接，避免目录链接跳出项目沙箱。
            List<Path> allFiles;
            try (Stream<Path> pathStream = Files.walk(path)) {
                allFiles = pathStream
                        .filter(file -> !file.equals(path))
                        .filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                        .filter(file -> !shouldIgnore(path, file))
                        .toList();
            }
            // 按路径深度和名称排序显示
            allFiles.stream()
                    .sorted((f1, f2) -> {
                        int depth1 = getRelativeDepth(path, f1);
                        int depth2 = getRelativeDepth(path, f2);
                        if (depth1 != depth2) {
                            return Integer.compare(depth1, depth2);
                        }
                        return f1.toString().compareTo(f2.toString());
                    })
                    .forEach(file -> {
                        int depth = getRelativeDepth(path, file);
                        String indent = "  ".repeat(depth);
                        structure.append(indent).append(file.getFileName()).append("\n");
                    });
            return structure.toString();

        } catch (IllegalArgumentException e) {
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.warn(errorMessage);
            return errorMessage;
        } catch (IOException e) {
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 计算文件相对于根目录的深度
     */
    private int getRelativeDepth(Path root, Path file) {
        return root.relativize(file).getNameCount() - 1;
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(Path root, Path file) {
        Path relativePath = root.relativize(file);
        return ProjectFileSecurityUtil.shouldIgnore(relativePath);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.getStr("relativeDirPath");
        if (StrUtil.isEmpty(relativeDirPath)) {
            relativeDirPath = "根目录";
        }
        return String.format("[工具调用] %s %s", getDisplayName(), relativeDirPath);
    }
}

