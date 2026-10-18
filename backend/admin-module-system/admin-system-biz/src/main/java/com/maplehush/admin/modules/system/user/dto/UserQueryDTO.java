package com.maplehush.admin.modules.system.user.dto;

/** 用户列表查询条件 */
public record UserQueryDTO(
        int page,
        int pageSize,
        String username,
        String realName,
        Long deptId,
        String status,
        Long userId,
        String remark,
        String startTime,
        String endTime) {}
