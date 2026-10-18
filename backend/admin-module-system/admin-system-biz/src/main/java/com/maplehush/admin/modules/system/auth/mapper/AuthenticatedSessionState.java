package com.maplehush.admin.modules.system.auth.mapper;

import java.time.LocalDateTime;

/** 当前访问令牌对应的实时用户和会话状态。 */
public record AuthenticatedSessionState(
        Long userId,
        Long sessionId,
        Integer status,
        Boolean mustChangePassword,
        LocalDateTime expireTime,
        LocalDateTime revokeTime) {}
