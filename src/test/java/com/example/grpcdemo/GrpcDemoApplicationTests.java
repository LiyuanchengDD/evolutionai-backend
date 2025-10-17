package com.example.grpcdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GrpcDemoApplicationTests {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
        assertThat(mailSender).isNotNull();
    }
}
