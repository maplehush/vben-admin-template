package com.maplehush.admin.modules.system.menu.service;

import java.util.Map;

/** 菜单及按钮资源维护 */
public interface ISysMenuService {
    public long create(long actor, String type, String key, String authCode, MenuData data);

    public void update(long actor, long id, MenuData data);

    public void delete(long actor, long id);

    public record MenuData(
            Long parentId,
            String routeName,
            String routePath,
            String component,
            String redirectPath,
            String titleKey,
            String icon,
            int sortOrder,
            Map<String, Object> routeMeta,
            String status) {}
}
