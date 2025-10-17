package com.example.grpcdemo.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        if ("*".equals(origins)) {
            cfg.addAllowedOriginPattern(CorsConfiguration.ALL);
        } else {
            for (String origin : origins.split(",")) {
                cfg.addAllowedOriginPattern(origin.trim());
            }
        }
        cfg.addAllowedMethod(CorsConfiguration.ALL);
        cfg.addAllowedHeader(CorsConfiguration.ALL);
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
