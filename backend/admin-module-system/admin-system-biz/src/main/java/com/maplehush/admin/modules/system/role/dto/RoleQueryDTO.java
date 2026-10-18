package com.maplehush.admin.modules.system.role.dto;

/** 角色列表查询条件 */
public record RoleQueryDTO(
        int page,
        int pageSize,
        Long roleId,
        String roleName,
        String status,
        String description,
        String startTime,
        String endTime) {}
