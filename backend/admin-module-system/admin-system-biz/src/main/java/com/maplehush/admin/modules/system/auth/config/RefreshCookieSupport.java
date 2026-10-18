package com.maplehush.admin.modules.system.auth.config;

import com.maplehush.admin.modules.system.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieSupport {

    private final AuthProperties properties;

    public RefreshCookieSupport(AuthProperties properties) {
        this.properties = properties;
    }

    public void requireAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || !properties.allowedOrigins().contains(origin)) {
            throw ApiException.forbiddenOrigin();
        }
    }

    public void set(HttpServletResponse response, String token) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookie(token, properties.refreshTtl()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, cookie("", java.time.Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, java.time.Duration maxAge) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Lax")
                .path(properties.cookiePath())
                .maxAge(maxAge)
                .build();
    }
}
