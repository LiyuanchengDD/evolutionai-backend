package com.example.grpcdemo.config;

import com.example.grpcdemo.storage.SupabaseStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SupabaseStorageProperties.class)
public class CommonConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}

