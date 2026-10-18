package com.maplehush.admin.modules.system.common.web;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.config.RefreshCookieSupport;
import com.maplehush.admin.modules.system.common.exception.ApiErrorCode;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final RefreshCookieSupport cookies;

    public GlobalExceptionHandler(RefreshCookieSupport cookies) {
        this.cookies = cookies;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResult<Void>> handleApiException(ApiException exception, HttpServletResponse response) {
        if (exception.code() == 40102) {
            cookies.clear(response);
        }
        return ResponseEntity.status(exception.status()).body(ApiResult.fail(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResult<Void>> handleBadRequest(Exception exception) {
        return failure(ApiErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResult<Void>> handleNotAcceptable() {
        return failure(ApiErrorCode.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Void>> handleUploadTooLarge() {
        return failure(ApiErrorCode.IMAGE_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled API error", exception);
        return failure(ApiErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiResult<Void>> failure(ApiErrorCode error) {
        return ResponseEntity.status(error.status()).body(ApiResult.fail(error.code(), error.message()));
    }
}
