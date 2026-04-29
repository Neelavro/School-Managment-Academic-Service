package com.example.academic_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Maps /images/** URLs to the local folder
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/var/www/student-service-images/");
    }
}