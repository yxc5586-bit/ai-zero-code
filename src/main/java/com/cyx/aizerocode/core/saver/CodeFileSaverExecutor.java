package com.cyx.aizerocode.core.saver;

import com.cyx.aizerocode.ai.model.HtmlCodeResult;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码保存执行器
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaverTemplate = new MultiFileCodeFileSaverTemplate();

    /**
     * 执行保存代码
     *
     * @param codeResult      代码结果对象
     * @param codeGenTypeEnum 代码生成类型
     * @return 保存的文件
     */
    public static File executorSaver(Object codeResult, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) codeResult);
            case MULTI_FILE -> multiFileCodeFileSaverTemplate.saveCode((MultiFileCodeResult) codeResult);
            default -> throw new IllegalArgumentException("Invalid code generation type: " + codeGenTypeEnum);
        };

    }
}
