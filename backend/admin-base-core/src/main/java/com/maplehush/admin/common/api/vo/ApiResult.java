package com.maplehush.admin.common.api.vo;

/** 统一 HTTP 响应，字段名遵循前端的 code、data、message 契约 */
public record ApiResult<T>(int code, T data, String message) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, data, "ok");
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, null, message);
    }
}
