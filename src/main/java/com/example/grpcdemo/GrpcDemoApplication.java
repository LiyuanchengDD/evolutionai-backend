// src/main/java/com/example/grpcdemo/GrpcDemoApplication.java
package com.example.grpcdemo;

import com.example.grpcdemo.auth.AuthManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.example.grpcdemo",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = AuthManager.class
    )
)
public class GrpcDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrpcDemoApplication.class, args);
    }
}
