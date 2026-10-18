package com.maplehush.admin.modules.system.menu.dto;

import com.maplehush.admin.modules.system.common.web.RequestIds;
import com.maplehush.admin.modules.system.menu.service.ISysMenuService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** 新增菜单请求数据 */
public record CreateMenuDTO(
        String parentId,
        @NotBlank String type,
        @NotBlank String menuKey,
        String routeName,
        String routePath,
        String component,
        String redirectPath,
        String authCode,
        @NotBlank @Size(max = 255) String titleKey,
        String icon,
        @NotNull Integer sortOrder,
        Map<String, Object> routeMeta,
        @NotBlank String status) {
    public ISysMenuService.MenuData data() {
        return new ISysMenuService.MenuData(
                RequestIds.optional(parentId),
                routeName,
                routePath,
                component,
                redirectPath,
                titleKey,
                icon,
                sortOrder,
                routeMeta,
                status);
    }
}
