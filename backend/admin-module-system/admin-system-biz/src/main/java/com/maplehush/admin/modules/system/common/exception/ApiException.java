package com.maplehush.admin.modules.system.common.exception;

import org.springframework.http.HttpStatus;

/** 可安全返回给客户端的业务异常 */
public class ApiException extends RuntimeException {
    private final ApiErrorCode error;

    public ApiException(ApiErrorCode error) {
        super(error.message());
        this.error = error;
    }

    public HttpStatus status() {
        return error.status();
    }

    public int code() {
        return error.code();
    }

    public static ApiException captchaInvalid() {
        return new ApiException(ApiErrorCode.CAPTCHA_INVALID);
    }

    public static ApiException invalidCredentials() {
        return new ApiException(ApiErrorCode.INVALID_CREDENTIALS);
    }

    public static ApiException invalidRefreshToken() {
        return new ApiException(ApiErrorCode.INVALID_REFRESH_TOKEN);
    }

    public static ApiException invalidAccessToken() {
        return new ApiException(ApiErrorCode.INVALID_ACCESS_TOKEN);
    }

    public static ApiException forbidden() {
        return new ApiException(ApiErrorCode.FORBIDDEN);
    }

    public static ApiException notFound() {
        return new ApiException(ApiErrorCode.NOT_FOUND);
    }

    public static ApiException invalidRequest() {
        return new ApiException(ApiErrorCode.INVALID_REQUEST);
    }

    public static ApiException conflict() {
        return new ApiException(ApiErrorCode.CONFLICT);
    }

    public static ApiException passwordChangeRequired() {
        return new ApiException(ApiErrorCode.PASSWORD_CHANGE_REQUIRED);
    }

    public static ApiException wrongOldPassword() {
        return new ApiException(ApiErrorCode.WRONG_OLD_PASSWORD);
    }

    public static ApiException invalidNewPassword() {
        return new ApiException(ApiErrorCode.INVALID_NEW_PASSWORD);
    }

    public static ApiException disabledUser() {
        return new ApiException(ApiErrorCode.DISABLED_USER);
    }

    public static ApiException forbiddenOrigin() {
        return new ApiException(ApiErrorCode.FORBIDDEN_ORIGIN);
    }

    public static ApiException rateLimited() {
        return new ApiException(ApiErrorCode.RATE_LIMITED);
    }

    public static ApiException uploadUnavailable() {
        return new ApiException(ApiErrorCode.UPLOAD_UNAVAILABLE);
    }

    public static ApiException fileReadFailure() {
        return new ApiException(ApiErrorCode.FILE_READ_FAILURE);
    }

    public static ApiException uploadFailure() {
        return new ApiException(ApiErrorCode.UPLOAD_FAILURE);
    }
}
