package com.example.grpcdemo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app.trial")
public class AppTrialProperties {

    private Mode mode = Mode.DEV_FIXED;
    private String env = "dev";
    private int validDays = 14;
    private final Dev dev = new Dev();

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public int getValidDays() {
        return validDays;
    }

    public void setValidDays(int validDays) {
        this.validDays = validDays;
    }

    public Dev getDev() {
        return dev;
    }

    public boolean isDevFixedMode() {
        return mode == Mode.DEV_FIXED;
    }

    public boolean isProdMode() {
        return mode == Mode.PROD;
    }

    @PostConstruct
    void validate() {
        if (isDevFixedMode()) {
            Assert.state(!"prod".equalsIgnoreCase(env), "生产环境禁止启用 dev-fixed 邀请码模式");
        }
    }

    public enum Mode {
        DEV_FIXED,
        PROD
    }

    public static final class Dev {
        private String code = "EA-TRIAL-123456";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
