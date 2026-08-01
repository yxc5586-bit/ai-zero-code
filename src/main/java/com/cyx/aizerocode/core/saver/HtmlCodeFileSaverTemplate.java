package com.cyx.aizerocode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.cyx.aizerocode.ai.model.HtmlCodeResult;
import com.cyx.aizerocode.ai.model.enums.CodeGenTypeEnum;
import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;

/**
 * html代码保存模板
 *
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {



    /**
     * 保存代码文件
     *
     * @param result  代码结果
     * @param dirPath 文件保存的基础路径
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String dirPath) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }


    /**
     * 获取输入的类型
     *
     * @return 输入的类型
     */
    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        //HTML代码不为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"HTML代码不能为空");
        }
    }
}
