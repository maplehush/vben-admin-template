package com.maplehush.admin.modules.system.audit.mapper;

import java.time.LocalDateTime;
import java.util.Set;

/** 日志查询筛选条件及操作人的数据范围。 */
public record AuditFilter(
        boolean all,
        long actorId,
        Set<Long> deptIds,
        String username,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean success,
        String actionCode) {}
