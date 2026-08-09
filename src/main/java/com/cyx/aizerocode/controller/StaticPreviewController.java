package com.cyx.aizerocode.controller;

import com.cyx.aizerocode.exception.BusinessException;
import com.cyx.aizerocode.exception.ErrorCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 为生成的网站目录提供默认首页，确保 /static/{应用目录}/ 可以直接访问。
 */
@Controller
public class StaticPreviewController {

    @GetMapping("/static/{directory}/")
    public String previewIndex(@PathVariable String directory) {
        if (!directory.matches("^(html|multi_file)_\\d+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的应用预览目录");
        }
        return "forward:/static/" + directory + "/index.html";
    }
}
