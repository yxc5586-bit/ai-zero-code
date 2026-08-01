package com.cyx.aizerocode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.ai.model.MultiFileCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;

import java.io.File;

/**
 * 多文件代码保存模板
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{


    /**
     * 保存多文件代码
     *
     * @param result  代码结果
     * @param dirPath 文件保存的基础路径
     */
    @Override
    protected void saveFiles(MultiFileCodeResult result, String dirPath) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());

    }

    /**
     * 获取输入的类型
     *
     * @return 输入的类型
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    /**
     * 验证输入
     * @param result 输入
     */
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        //至少HTML代码不为空，其他类型代码可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML代码不能为空");
        }
    }
}
