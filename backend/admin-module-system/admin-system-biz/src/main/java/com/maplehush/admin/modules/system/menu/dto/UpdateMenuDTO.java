package com.maplehush.admin.modules.system.menu.dto;

import com.maplehush.admin.modules.system.common.web.RequestIds;
import com.maplehush.admin.modules.system.menu.service.ISysMenuService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/** 更新菜单请求数据 */
public record UpdateMenuDTO(
        String parentId,
        String routeName,
        String routePath,
        String component,
        String redirectPath,
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
