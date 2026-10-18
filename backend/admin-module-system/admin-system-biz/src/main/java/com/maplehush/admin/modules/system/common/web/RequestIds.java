package com.maplehush.admin.modules.system.common.web;

import com.maplehush.admin.modules.system.common.exception.ApiException;

/** HTTP 请求中正整数 ID 的解析规则 */
public final class RequestIds {
    private RequestIds() {}

    public static Long optional(String value) {
        return value == null ? null : required(value);
    }

    public static long required(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw ApiException.invalidRequest();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw ApiException.invalidRequest();
        }
    }
}
