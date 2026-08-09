package com.cyx.aizerocode.ai;


import com.cyx.aizerocode.ai.model.HtmlCodeResult;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务接口
 */
@Service
public interface AiCodeGeneratorService {

    /**
     * 生成html代码
     * @param message 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String message);

    /**
     * 生成多个文件代码
     * @param message 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String message);

    /**
     *  流式生成html代码
     * @param message 用户消息
     * @return 流式生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String message);

    /**
     *  流式生成多个文件代码
     * @param message 用户消息
     * @return 流式生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String message);


    /**
     *  流式生成 Vue 工程代码
     * @param message 用户消息
     * @return 流式生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@UserMessage String message, @MemoryId long appId);

}
