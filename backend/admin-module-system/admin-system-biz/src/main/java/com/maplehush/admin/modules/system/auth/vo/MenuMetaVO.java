package com.maplehush.admin.modules.system.auth.vo;

/** 菜单元信息响应视图 */
public record MenuMetaVO(
        String title,
        String icon,
        int order,
        String iframeSrc,
        String link,
        String activeIcon,
        String badgeType,
        String badge,
        String badgeVariants,
        Boolean keepAlive,
        Boolean affixTab,
        Boolean hideInMenu,
        Boolean hideChildrenInMenu,
        Boolean hideInBreadcrumb,
        Boolean hideInTab) {}
