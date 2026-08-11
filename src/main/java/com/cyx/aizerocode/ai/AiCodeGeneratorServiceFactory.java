package com.cyx.aizerocode.ai;

import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.ai.tools.FileDirReadTool;
import com.cyx.aizerocode.ai.tools.FileReadTool;
import com.cyx.aizerocode.ai.tools.FileWriteTool;
import com.cyx.aizerocode.ai.tools.ToolManager;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import com.cyx.aizerocode.service.ChatHistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum.*;

/**
 * AiCodeGeneratorService 的工厂类，用于按 appId 创建带独立会话记忆的 AI 服务实例。
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0);
    }

    @Resource
    private ToolManager toolManager;

    /**
     * 使用 Caffeine 缓存 AI 服务实例，避免重复创建
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause))
            .build();



    /**
     * 根据 appId 获取 AiCodeGeneratorService 实例(兼容旧方法)
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 和代码生成类型获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }



    /**
     * 创建新的 AI 服务实例。(兼容老方法)
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        return createAiCodeGeneratorService(appId, MULTI_FILE);
    }

    /**
     * 创建新的 AI 服务实例。
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId , CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

        // 加载会话记忆
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 10);

        return switch (codeGenType){
            // Vue 项目 使用不同模型
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools( toolManager.getAllTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();

            //
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .build();

            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType){
        return appId + "_" + codeGenType.getValue();
    }
}
