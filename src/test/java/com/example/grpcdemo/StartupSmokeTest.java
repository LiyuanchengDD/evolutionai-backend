package com.example.grpcdemo;

import com.example.grpcdemo.auth.AuthManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StartupSmokeTest {

    @Autowired
    private AuthManager authManager;

    @Test
    void authManagerBeanIsCreated() {
        assertThat(authManager).isNotNull();
    }
}
