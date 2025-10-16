package com.example.grpcdemo.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    private AuthMode mode = AuthMode.SUPABASE;
    private String env = "dev";
    private final Supabase supabase = new Supabase();
    private final DevOtp devOtp = new DevOtp();

    public AuthMode getMode() {
        return mode;
    }

    public void setMode(AuthMode mode) {
        this.mode = mode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public Supabase getSupabase() {
        return supabase;
    }

    public DevOtp getDevOtp() {
        return devOtp;
    }

    public boolean isSupabaseMode() {
        return mode == AuthMode.SUPABASE;
    }

    public boolean isDevOtpMode() {
        return mode == AuthMode.DEV_OTP;
    }

    @PostConstruct
    void validate() {
        if (isDevOtpMode()) {
            Assert.state(!"prod".equalsIgnoreCase(env),
                    "dev-otp 模式禁止在生产环境启用，请将 APP_AUTH_MODE 切换为 supabase");
        }
    }

    public enum AuthMode {
        SUPABASE,
        DEV_OTP
    }

    public static final class Supabase {
        private String projectUrl;
        private String jwksUrl;

        public String getProjectUrl() {
            return projectUrl;
        }

        public void setProjectUrl(String projectUrl) {
            this.projectUrl = projectUrl;
        }

        public String getJwksUrl() {
            return jwksUrl;
        }

        public void setJwksUrl(String jwksUrl) {
            this.jwksUrl = jwksUrl;
        }

        public String getIssuer() {
            return projectUrl != null ? projectUrl + "/auth/v1" : null;
        }
    }

    public static final class DevOtp {
        private String code = "123456";
        private String jwtSecret = "change-me-please";
        private int jwtExpiresMin = 1440;
        private String userId = "00000000-0000-0000-0000-000000000000";
        private String userEmail = "dev@example.com";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public int getJwtExpiresMin() {
            return jwtExpiresMin;
        }

        public void setJwtExpiresMin(int jwtExpiresMin) {
            this.jwtExpiresMin = jwtExpiresMin;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public void setUserEmail(String userEmail) {
            this.userEmail = userEmail;
        }

        public Duration getTokenTtl() {
            return Duration.ofMinutes(jwtExpiresMin);
        }
    }
}

