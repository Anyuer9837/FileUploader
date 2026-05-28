package com.yuer.fileuploader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 跨平台处理：将相对路径转为绝对路径的 URI
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(path.toUri().toString());
    }
}