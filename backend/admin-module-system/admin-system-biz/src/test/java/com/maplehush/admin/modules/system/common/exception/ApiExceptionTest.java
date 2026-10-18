package com.maplehush.admin.modules.system.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionTest {
    @Test
    void knownFailuresKeepTheirHttpStatusCodeAndMessage() {
        assertFailure(ApiException.captchaInvalid(), HttpStatus.BAD_REQUEST, 40001, "验证码错误或已过期");
        assertFailure(ApiException.invalidRefreshToken(), HttpStatus.UNAUTHORIZED, 40102, "登录会话已失效");
        assertFailure(ApiException.conflict(), HttpStatus.CONFLICT, 40901, "数据冲突或仍被引用");
        assertFailure(ApiException.uploadUnavailable(), HttpStatus.SERVICE_UNAVAILABLE, 50300, "文件上传服务未配置");
    }

    private void assertFailure(ApiException exception, HttpStatus status, int code, String message) {
        assertEquals(status, exception.status());
        assertEquals(code, exception.code());
        assertEquals(message, exception.getMessage());
    }
}
