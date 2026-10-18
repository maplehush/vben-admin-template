package com.maplehush.admin.modules.system.role.vo;

import java.time.LocalDateTime;

/** 角色列表项 */
public record RoleSummaryVO(
        String id,
        String roleCode,
        String roleName,
        String description,
        String status,
        String dataScopeType,
        LocalDateTime createTime) {}
