package com.cyx.aizerocode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 将 AI 生成的网站目录暴露为本地预览资源。
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final ZeroCodeProperties zeroCodeProperties;

    public StaticResourceConfig(ZeroCodeProperties zeroCodeProperties) {
        this.zeroCodeProperties = zeroCodeProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations(toDirectoryLocation(zeroCodeProperties.getCode().getOutputRoot()))
                .setCacheControl(CacheControl.noStore());
    }

    private String toDirectoryLocation(String directory) {
        String location = Paths.get(directory)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
