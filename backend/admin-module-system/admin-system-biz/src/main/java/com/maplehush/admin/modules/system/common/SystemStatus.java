package com.maplehush.admin.modules.system.common;

import com.maplehush.admin.common.constant.CommonStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;

/** 将公共启禁用状态接入系统管理的请求校验 */
public final class SystemStatus {
    private SystemStatus() {}

    public static int databaseValue(String apiValue) {
        try {
            return CommonStatus.fromApi(apiValue).databaseValue();
        } catch (IllegalArgumentException exception) {
            throw ApiException.invalidRequest();
        }
    }

    public static Integer databaseValueOrNull(String apiValue) {
        return apiValue == null ? null : databaseValue(apiValue);
    }

    public static String apiValue(int databaseValue) {
        return CommonStatus.fromDatabase(databaseValue).apiValue();
    }
}
