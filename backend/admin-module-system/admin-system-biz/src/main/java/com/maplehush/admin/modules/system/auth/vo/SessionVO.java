package com.maplehush.admin.modules.system.auth.vo;

import java.time.LocalDateTime;

/** 会话响应视图 */
public record SessionVO(
        String id,
        String username,
        String deviceInfo,
        String ipAddress,
        LocalDateTime createTime,
        LocalDateTime expireTime,
        boolean current) {
    /** 将数据库主键转换为接口 ID */
    public SessionVO(
            Long id,
            String username,
            String deviceInfo,
            String ipAddress,
            LocalDateTime createTime,
            LocalDateTime expireTime,
            boolean current) {
        this(id.toString(), username, deviceInfo, ipAddress, createTime, expireTime, current);
    }
}
