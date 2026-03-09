package com.example.learning_spring_security.Config.ConfigSecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedOrigins("https://e-shop-seven-swart.vercel.app/")
                        .allowedOrigins("http://localhost:5173/")
                        .allowedHeaders("**")
                        .allowCredentials(true)
                        .exposedHeaders("Authorization");
            }
        };
    }
}