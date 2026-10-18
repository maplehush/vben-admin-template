package com.maplehush.admin.modules.system.common.exception;

import org.springframework.http.HttpStatus;

/** 后端错误的 HTTP 状态、业务码和对外文案 */
public enum ApiErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 40000, "请求参数错误"),
    CAPTCHA_INVALID(HttpStatus.BAD_REQUEST, 40001, "验证码错误或已过期"),
    WRONG_OLD_PASSWORD(HttpStatus.BAD_REQUEST, 40000, "原密码错误"),
    INVALID_NEW_PASSWORD(HttpStatus.BAD_REQUEST, 40000, "新密码需为 8 至 128 位且不能与原密码相同"),
    FILE_READ_FAILURE(HttpStatus.BAD_REQUEST, 40000, "读取上传文件失败"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, 40100, "登录凭证无效或已过期"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 40101, "账号或密码错误"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, 40102, "登录会话已失效"),
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "无权访问"),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN, 40300, "请先修改初始密码"),
    DISABLED_USER(HttpStatus.FORBIDDEN, 40300, "账号已停用"),
    FORBIDDEN_ORIGIN(HttpStatus.FORBIDDEN, 40300, "请求来源不允许"),
    NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "对象不存在"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, 40600, "不支持的响应格式"),
    CONFLICT(HttpStatus.CONFLICT, 40901, "数据冲突或仍被引用"),
    IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, 41300, "图片不能超过 5 MB"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, 42900, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "服务器内部错误"),
    UPLOAD_FAILURE(HttpStatus.BAD_GATEWAY, 50200, "文件上传失败"),
    UPLOAD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 50300, "文件上传服务未配置");

    private final HttpStatus status;
    private final int code;
    private final String message;

    ApiErrorCode(HttpStatus status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
