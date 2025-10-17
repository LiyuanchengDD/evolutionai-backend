package com.example.grpcdemo.config;

import com.example.grpcdemo.security.AppAuthProperties;
import com.example.grpcdemo.security.CompositeJwtDecoder;
import com.example.grpcdemo.security.UserJwtAuthenticationConverter;
import com.example.grpcdemo.security.trial.TrialAccessFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppAuthProperties.class)
public class SecurityConfig {

    private final AppAuthProperties authProperties;

    public SecurityConfig(AppAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TrialAccessFilter trialAccessFilter,
                                                   JwtDecoder jwtDecoder) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**", "/api/health").permitAll()
                .requestMatchers("/auth/me").authenticated()
                .requestMatchers("/dev/auth/**").permitAll()
                .anyRequest().authenticated());

        http.oauth2ResourceServer(oauth -> oauth
                .authenticationEntryPoint(authenticationEntryPoint())
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        http.exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler()));

        http.addFilterAfter(trialAccessFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        List<JwtDecoder> decoders = new ArrayList<>();
        if (StringUtils.hasText(authProperties.getSupabase().getJwksUrl())) {
            decoders.add(buildSupabaseDecoder());
        }
        if (authProperties.isDevOtpMode()) {
            decoders.add(buildDevDecoder());
        }
        if (decoders.isEmpty()) {
            throw new IllegalStateException("未配置可用的 JWT 解码器，请检查 Supabase JWKS 或 dev-otp 设置");
        }
        return new CompositeJwtDecoder(decoders);
    }

    private JwtDecoder buildSupabaseDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(authProperties.getSupabase().getJwksUrl()).build();
        String issuer = authProperties.getSupabase().getIssuer();
        OAuth2TokenValidator<Jwt> validator = issuer != null
                ? new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer))
                : JwtValidators.createDefault();
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private JwtDecoder buildDevDecoder() {
        byte[] secretBytes = authProperties.getDevOtp().getJwtSecret().getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).build();
        decoder.setJwtValidator(token -> {
            String issuer = token.getIssuer() != null ? token.getIssuer().toString() : "";
            if (!issuer.startsWith("app://dev")) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid dev token issuer", null));
            }
            return OAuth2TokenValidatorResult.success();
        });
        return decoder;
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new UserJwtAuthenticationConverter();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            String message = "访问令牌无效";
            String code = "UNAUTHORIZED";
            Throwable cause = authException.getCause();
            if (cause != null && cause.getMessage() != null && cause.getMessage().toLowerCase().contains("expired")) {
                code = "TOKEN_EXPIRED";
                message = "访问令牌已过期";
            }
            try {
                response.getWriter().write("{\"errorCode\":\"" + code + "\",\"message\":\"" + message + "\"}");
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to write unauthorized response", ex);
            }
        };
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"errorCode\":\"FORBIDDEN\",\"message\":\"无权访问该资源\"}");
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to write forbidden response", ex);
            }
        };
    }
}
