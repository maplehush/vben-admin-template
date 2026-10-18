package com.maplehush.admin.modules.system.menu.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 菜单响应视图 */
public record MenuDetailVO(
        String id,
        String parentId,
        String type,
        String menuKey,
        String routeName,
        String routePath,
        String component,
        String redirectPath,
        String authCode,
        String titleKey,
        String icon,
        int sortOrder,
        Map<String, Object> routeMeta,
        String status,
        List<MenuNodeVO> children,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
