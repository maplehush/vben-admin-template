package com.maplehush.admin.modules.system.permission.dto;

import com.maplehush.admin.modules.system.common.web.RequestIds;
import com.maplehush.admin.modules.system.permission.service.ISysPermissionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 更新接口权限请求数据 */
public record UpdatePermissionDTO(
        String menuId,
        @NotBlank String titleKey,
        @NotNull Integer sortOrder,
        @NotBlank String status) {
    public ISysPermissionService.PermissionData data() {
        return new ISysPermissionService.PermissionData(
                menuId == null ? null : RequestIds.required(menuId), titleKey, sortOrder, status);
    }
}
