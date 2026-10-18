package com.maplehush.admin.modules.system.auth.config;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String jwtSecretBase64,
        Duration accessTtl,
        Duration refreshTtl,
        String cookiePath,
        boolean cookieSecure,
        List<String> allowedOrigins) {

    public AuthProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.auth.issuer is required");
        }
        if (jwtSecretBase64 == null || Base64.getDecoder().decode(jwtSecretBase64).length < 32) {
            throw new IllegalArgumentException("app.auth.jwt-secret-base64 must contain at least 32 bytes");
        }
        if (accessTtl == null
                || accessTtl.isNegative()
                || accessTtl.isZero()
                || refreshTtl == null
                || refreshTtl.compareTo(accessTtl) <= 0) {
            throw new IllegalArgumentException("Invalid app.auth token lifetimes");
        }
        if (cookiePath == null || !cookiePath.startsWith("/")) {
            throw new IllegalArgumentException("app.auth.cookie-path must start with /");
        }
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        if (allowedOrigins.isEmpty() || allowedOrigins.stream().anyMatch(origin -> !origin.startsWith("http"))) {
            throw new IllegalArgumentException("app.auth.allowed-origins must contain trusted origins");
        }
    }
}
