package com.maplehush.admin.modules.system.user.mapper;

import java.time.LocalDateTime;
import java.util.Set;

/** 用户列表筛选条件及调用者的数据范围。 */
public record SysUserQueryFilter(
        boolean all,
        long actorId,
        Set<Long> deptIds,
        String username,
        String realName,
        String remark,
        Long deptId,
        Long userId,
        Integer status,
        LocalDateTime startTime,
        LocalDateTime endTime) {}
