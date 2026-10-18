package com.maplehush.admin.modules.system.role.vo;

import java.time.LocalDateTime;

/** 角色详情 */
public record RoleDetailVO(
        String id,
        String roleCode,
        String roleName,
        String description,
        String status,
        String dataScopeType,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
    public RoleDetailVO(RoleSummaryVO base, LocalDateTime updateTime) {
        this(
                base.id(),
                base.roleCode(),
                base.roleName(),
                base.description(),
                base.status(),
                base.dataScopeType(),
                base.createTime(),
                updateTime);
    }
}
