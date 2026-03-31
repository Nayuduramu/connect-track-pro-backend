package com.connecttrack.pro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps requests like http://.../user-uploads/profile-photos/image.jpg
        // to the file system directory C:/.../api/user-uploads/profile-photos/image.jpg
        String resourcePath = "/" + uploadDir.replace("\\", "/") + "/**";
        String resourceLocation = "file:" + uploadDir.replace("\\", "/") + "/";

        registry.addResourceHandler(resourcePath).addResourceLocations(resourceLocation);
    }
}