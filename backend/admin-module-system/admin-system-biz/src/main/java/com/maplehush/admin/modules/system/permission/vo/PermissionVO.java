package com.maplehush.admin.modules.system.permission.vo;

import java.time.LocalDateTime;

/** 接口权限响应视图 */
public record PermissionVO(
        String id,
        String menuId,
        String permissionKey,
        String permissionCode,
        String titleKey,
        int sortOrder,
        String status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
