package com.cyx.aizerocode.core.parser;

import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 解析代码
     *
     * @param codeContent      代码内容
     * @param codeGenTypeEnum 代码生成类型
     * @return 解析结果对象(HTMLCodeResult 或 MultiFileCodeResult)
     */
    public static Object parseCode(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new IllegalArgumentException("Invalid code generation type: " + codeGenTypeEnum);
        };

    }
}
