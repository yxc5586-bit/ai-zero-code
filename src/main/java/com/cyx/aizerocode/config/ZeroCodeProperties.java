package com.cyx.aizerocode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ZeroCode 单机演示运行配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "zerocode")
public class ZeroCodeProperties {

    private Code code = new Code();

    private Screenshot screenshot = new Screenshot();

    private Generation generation = new Generation();

    @Data
    public static class Code {

        private String outputRoot = System.getProperty("user.dir") + "/tmp/code_output";

        private String deployRoot = System.getProperty("user.dir") + "/tmp/code_deploy";

        private String publicBaseUrl = "http://localhost";

        public String getPublicBaseUrl() {
            if (publicBaseUrl == null || publicBaseUrl.isBlank() || publicBaseUrl.length() == 1) {
                return publicBaseUrl;
            }
            return publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        }
    }

    @Data
    public static class Screenshot {

        private boolean enabled = true;

        private String tempRoot = System.getProperty("user.dir") + "/tmp/screenshots";

        private String chromeBinaryPath = "";

        private String chromeDriverPath = "";
    }

    @Data
    public static class Generation {

        private int globalConcurrency = 2;
    }
}
