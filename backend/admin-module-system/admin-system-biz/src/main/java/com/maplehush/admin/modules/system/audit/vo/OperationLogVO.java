package com.maplehush.admin.modules.system.audit.vo;

import java.time.LocalDateTime;

/** 操作日志响应视图 */
public record OperationLogVO(
        String id,
        String username,
        String actionCode,
        String targetType,
        String targetId,
        boolean success,
        String ipAddress,
        LocalDateTime occurTime) {
    /** 将数据库主键转换为接口 ID */
    public OperationLogVO(
            Long id,
            String username,
            String actionCode,
            String targetType,
            String targetId,
            boolean success,
            String ipAddress,
            LocalDateTime occurTime) {
        this(id.toString(), username, actionCode, targetType, targetId, success, ipAddress, occurTime);
    }
}
