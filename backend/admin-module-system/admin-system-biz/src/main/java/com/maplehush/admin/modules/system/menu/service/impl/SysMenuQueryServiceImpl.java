package com.maplehush.admin.modules.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import com.maplehush.admin.modules.system.menu.mapper.SysMenuMapper;
import com.maplehush.admin.modules.system.menu.service.ISysMenuQueryService;
import com.maplehush.admin.modules.system.menu.vo.MenuDetailVO;
import com.maplehush.admin.modules.system.menu.vo.MenuNodeVO;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SysMenuQueryServiceImpl implements ISysMenuQueryService {
    private final SysMenuMapper menus;
    private final IManagementPermissionService permissions;
    private final ObjectMapper json;

    public SysMenuQueryServiceImpl(SysMenuMapper menus, IManagementPermissionService permissions, ObjectMapper json) {
        this.menus = menus;
        this.permissions = permissions;
        this.json = json;
    }

    @Override
    public List<MenuNodeVO> tree(long actor) {
        permissions.require(actor, "system:menu:list");
        return nodes().roots();
    }

    @Override
    public MenuDetailVO detail(long actor, long id) {
        permissions.require(actor, "system:menu:view");
        MenuTree tree = nodes();
        MenuNodeVO node = tree.byId().get(id);
        if (node == null) throw ApiException.notFound();
        SysMenu times = menus.selectById(id);
        return new MenuDetailVO(
                node.id(),
                node.parentId(),
                node.type(),
                node.menuKey(),
                node.routeName(),
                node.routePath(),
                node.component(),
                node.redirectPath(),
                node.authCode(),
                node.titleKey(),
                node.icon(),
                node.sortOrder(),
                node.routeMeta(),
                node.status(),
                node.children(),
                times.getCreateTime(),
                times.getUpdateTime());
    }

    private MenuTree nodes() {
        List<SysMenu> rows =
                menus.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder, SysMenu::getId));
        Map<Long, MenuNodeVO> byId = new HashMap<>();
        for (SysMenu row : rows) {
            byId.put(
                    row.getId(),
                    new MenuNodeVO(
                            Long.toString(row.getId()),
                            row.getParentId() == null ? null : row.getParentId().toString(),
                            row.getMenuType(),
                            row.getMenuKey(),
                            row.getRouteName(),
                            row.getRoutePath(),
                            row.getComponent(),
                            row.getRedirectPath(),
                            row.getAuthCode(),
                            row.getTitleKey(),
                            row.getIcon(),
                            row.getSortOrder(),
                            routeMeta(row.getRouteMeta()),
                            SystemStatus.apiValue(row.getStatus()),
                            new ArrayList<>()));
        }
        List<MenuNodeVO> roots = new ArrayList<>();
        for (SysMenu row : rows) {
            MenuNodeVO node = byId.get(row.getId());
            MenuNodeVO parent = row.getParentId() == null ? null : byId.get(row.getParentId());
            if (parent == null) roots.add(node);
            else parent.children().add(node);
        }
        Comparator<MenuNodeVO> order =
                Comparator.comparingInt(MenuNodeVO::sortOrder).thenComparingLong(node -> Long.parseLong(node.id()));
        sort(roots, order);
        return new MenuTree(roots, byId);
    }

    private void sort(List<MenuNodeVO> nodes, Comparator<MenuNodeVO> order) {
        nodes.sort(order);
        nodes.forEach(node -> sort(node.children(), order));
    }

    private Map<String, Object> routeMeta(String value) {
        if (value == null) return null;
        try {
            Map<?, ?> raw = json.readValue(value, Map.class);
            Map<String, Object> approved = new LinkedHashMap<>();
            if (raw.get("title") instanceof String title) approved.put("title", title);
            if (raw.get("icon") instanceof String icon) approved.put("icon", icon);
            if (raw.get("order") instanceof Number order) approved.put("order", order.intValue());
            if (raw.get("iframeSrc") instanceof String iframeSrc) approved.put("iframeSrc", iframeSrc);
            if (raw.get("link") instanceof String link) approved.put("link", link);
            for (String key : List.of("activeIcon", "badgeType", "badge", "badgeVariants")) {
                if (raw.get(key) instanceof String text) approved.put(key, text);
            }
            for (String key : List.of(
                    "keepAlive", "affixTab", "hideInMenu", "hideChildrenInMenu", "hideInBreadcrumb", "hideInTab")) {
                if (raw.get(key) instanceof Boolean flag) approved.put(key, flag);
            }
            return approved;
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored menu metadata", exception);
        }
    }

    private record MenuTree(List<MenuNodeVO> roots, Map<Long, MenuNodeVO> byId) {}
}
