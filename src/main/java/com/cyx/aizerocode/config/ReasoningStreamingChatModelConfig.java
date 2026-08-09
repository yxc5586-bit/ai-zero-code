package com.cyx.aizerocode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    @Bean
    public StreamingChatModel reasoningStreamingChatModel(){
        final String modelName = "deepseek-V4-flash";

        final int maxTokens = 65536;


        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl).
                apiKey(apiKey).
                modelName(modelName).
                maxTokens(maxTokens)
                .build();
    }

}
