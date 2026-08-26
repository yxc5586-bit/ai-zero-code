package com.cyx.aizerocode.constant;

/**
 * 应用常量
 *
 * @author 25038
 * @since 2026-08-02
 */
public interface AppConstant {

    /**
     * 精选应用的优先级
     */
    Integer GOOD_APP_PRIORITY = 99;

    /**
     * 默认应用优先级
     */
    Integer DEFAULT_APP_PRIORITY = 0;

    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = env("CODE_OUTPUT_ROOT", System.getProperty("user.dir") + "/tmp/code_output");

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = env("CODE_DEPLOY_ROOT", System.getProperty("user.dir") + "/tmp/code_deploy");

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = trimTrailingSlash(env("PUBLIC_BASE_URL", env("CODE_DEPLOY_HOST", "http://localhost")));

    /**
     * 网页截图临时目录
     */
    String SCREENSHOT_TEMP_ROOT_DIR = env("SCREENSHOT_TEMP_ROOT", System.getProperty("user.dir") + "/tmp/screenshots");

    /**
     * Chrome / Chromium 可执行文件路径，为空时使用系统默认发现逻辑
     */
    String CHROME_BINARY_PATH = env("CHROME_BINARY_PATH", "");

    /**
     * ChromeDriver 路径，为空时使用 WebDriverManager 自动管理
     */
    String CHROME_DRIVER_PATH = env("CHROME_DRIVER_PATH", "");

    /**
     * 演示服务器生成并发上限
     */
    Integer GENERATION_GLOBAL_CONCURRENCY = Integer.parseInt(env("GENERATION_GLOBAL_CONCURRENCY", "1"));

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank() || value.length() == 1) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

}
