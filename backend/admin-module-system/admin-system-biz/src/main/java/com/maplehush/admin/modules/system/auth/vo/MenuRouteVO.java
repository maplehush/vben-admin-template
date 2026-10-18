package com.maplehush.admin.modules.system.auth.vo;

import java.util.List;

/** 菜单路由响应视图 */
public record MenuRouteVO(
        String name, String path, String component, String redirect, MenuMetaVO meta, List<MenuRouteVO> children) {}
