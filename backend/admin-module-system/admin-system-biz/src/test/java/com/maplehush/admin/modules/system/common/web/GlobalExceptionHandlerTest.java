package com.maplehush.admin.modules.system.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.modules.system.auth.config.AuthProperties;
import com.maplehush.admin.modules.system.auth.config.RefreshCookieSupport;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new RefreshCookieSupport(new AuthProperties(
                    "test",
                    Base64.getEncoder().encodeToString(new byte[32]),
                    Duration.ofMinutes(15),
                    Duration.ofDays(7),
                    "/auth",
                    false,
                    List.of("http://localhost"))));

    @Test
    void refreshFailureKeepsErrorEnvelopeAndClearsCookie() {
        var response = new MockHttpServletResponse();
        var result = handler.handleApiException(ApiException.invalidRefreshToken(), response);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(40102, result.getBody().code());
        assertNull(result.getBody().data());
        assertEquals("登录会话已失效", result.getBody().message());
        assertTrue(response.getHeader("Set-Cookie").contains("refresh_token="));
    }

    @Test
    void fallbackAndUploadErrorsKeepPublicContract() {
        var fallback = handler.handleUnexpected(new IllegalStateException("sensitive detail"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, fallback.getStatusCode());
        assertEquals(50000, fallback.getBody().code());
        assertEquals("服务器内部错误", fallback.getBody().message());
        var upload = handler.handleUploadTooLarge();
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, upload.getStatusCode());
        assertEquals(41300, upload.getBody().code());
        assertEquals("图片不能超过 5 MB", upload.getBody().message());
    }
}
