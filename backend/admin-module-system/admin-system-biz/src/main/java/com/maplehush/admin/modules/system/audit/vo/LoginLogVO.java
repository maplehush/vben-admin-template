package com.maplehush.admin.modules.system.audit.vo;

import java.time.LocalDateTime;

/** 登录日志响应视图 */
public record LoginLogVO(
        String id,
        String username,
        boolean success,
        String failureReason,
        String ipAddress,
        String userAgent,
        LocalDateTime occurTime) {}
