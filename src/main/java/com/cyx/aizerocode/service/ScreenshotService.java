package com.cyx.aizerocode.service;

/**
 * 对话历史 服务层。
 *
 * @author 25038
 * @since 2026-08-05
 */
public interface ScreenshotService {

    /**
     * 生成并上传截图。
     *
     * @param webUrl 网站地址。
     * @return 截图的访问地址。
     */
    String generateAndUploadScreenshot(String webUrl);
}
