package com.cyx.aizerocode.config;

import com.cyx.aizerocode.constant.AppConstant;
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations(toDirectoryLocation(AppConstant.CODE_OUTPUT_ROOT_DIR))
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
