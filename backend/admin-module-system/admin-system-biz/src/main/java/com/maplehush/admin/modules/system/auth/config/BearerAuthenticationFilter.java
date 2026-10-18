package com.maplehush.admin.modules.system.auth.config;

import com.maplehush.admin.modules.system.auth.service.ICurrentUserService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerAuthenticationFilter extends OncePerRequestFilter {

    private final ICurrentUserService currentUsers;

    public BearerAuthenticationFilter(ICurrentUserService currentUsers) {
        this.currentUsers = currentUsers;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals("/auth/captcha")
                || path.equals("/auth/login")
                || path.equals("/auth/refresh")
                || path.equals("/auth/logout")
                || path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ")
                    || authorization.substring(7).isBlank()) {
                writeError(response, ApiException.invalidAccessToken());
                return;
            }
            try {
                ICurrentUserService.CurrentUser user = currentUsers.authenticate(authorization.substring(7));
                if (user.mustChangePassword() && !path.equals("/user/info") && !path.equals("/user/password")) {
                    writeError(response, ApiException.passwordChangeRequired());
                    return;
                }
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            } catch (ApiException exception) {
                writeError(response, exception);
                return;
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public static void writeError(HttpServletResponse response, ApiException exception) throws IOException {
        response.setStatus(exception.status().value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter()
                .write("{\"code\":" + exception.code()
                        + ",\"data\":null,\"message\":\""
                        + exception.getMessage() + "\"}");
    }
}
