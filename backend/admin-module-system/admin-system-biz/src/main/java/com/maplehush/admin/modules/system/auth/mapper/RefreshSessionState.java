package com.maplehush.admin.modules.system.auth.mapper;

import java.time.LocalDateTime;

/** 刷新令牌锁定时读取的会话和用户状态。 */
public record RefreshSessionState(
        Long id, Long userId, LocalDateTime expireTime, LocalDateTime revokeTime, Integer status) {}
