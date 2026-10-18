package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.modules.system.auth.mapper.UserAccessMapper;
import com.maplehush.admin.modules.system.auth.service.BundledPageComponents;
import com.maplehush.admin.modules.system.auth.service.IUserAccessService;
import com.maplehush.admin.modules.system.auth.vo.MenuMetaVO;
import com.maplehush.admin.modules.system.auth.vo.MenuRouteVO;
import com.maplehush.admin.modules.system.auth.vo.UserInfoVO;
import com.maplehush.admin.modules.system.common.constant.SystemConstants;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.entity.SysPermission;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** 根据当前有效角色生成菜单路由与按钮、接口权限码。 */
@Service
public class UserAccessServiceImpl implements IUserAccessService {

    private final UserAccessMapper access;
    private final ObjectMapper json;

    public UserAccessServiceImpl(UserAccessMapper access, ObjectMapper json) {
        this.access = access;
        this.json = json;
    }

    @Override
    public UserInfoVO info(long userId) {
        SysUser user = access.userInfo(userId);
        if (user == null) {
            throw ApiException.invalidAccessToken();
        }
        List<String> roles = roles(userId);
        List<MenuRouteVO> routes = menus(userId);
        String homePath = firstPagePath(routes);
        if (user.getHomePath() != null && routeContains(routes, user.getHomePath())) {
            homePath = user.getHomePath();
        }
        return new UserInfoVO(
                Long.toString(user.getId()),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl() == null ? "" : user.getAvatarUrl(),
                roles,
                homePath,
                "",
                user.getMustChangePassword() == 1);
    }

    @Override
    public List<String> codes(long userId) {
        Set<Long> allowedMenus = grantedMenuIds(userId);
        Set<Long> allowedPermissions = grantedPermissionIds(userId);
        boolean superAdmin = roles(userId).contains(SystemConstants.SUPER_ADMIN_ROLE_CODE);
        TreeSet<String> codes = new TreeSet<>();
        List<Menu> menus = menuRows();
        Map<Long, Menu> byId = new HashMap<>();
        menus.forEach(menu -> byId.put(menu.id(), menu));
        for (Menu menu : menus) {
            if ("BUTTON".equals(menu.type())
                    && menu.authCode() != null
                    && (superAdmin || allowedMenus.contains(menu.id()))
                    && activeLineage(menu, byId) != null) {
                codes.add(menu.authCode());
            }
        }
        for (SysPermission permission : access.activePermissions()) {
            if (superAdmin || allowedPermissions.contains(permission.getId())) {
                codes.add(permission.getPermissionCode());
            }
        }
        return List.copyOf(codes);
    }

    @Override
    public List<MenuRouteVO> menus(long userId) {
        List<Menu> all = menuRows();
        Map<Long, Menu> byId = new HashMap<>();
        all.forEach(menu -> byId.put(menu.id(), menu));
        Set<Long> granted = grantedMenuIds(userId);
        boolean superAdmin = roles(userId).contains(SystemConstants.SUPER_ADMIN_ROLE_CODE);
        Set<Long> selected = new HashSet<>();
        for (Menu menu : all) {
            if (!("MENU".equals(menu.type()) && BundledPageComponents.contains(menu.component())
                            || "EMBEDDED".equals(menu.type()) && stringMeta(menu.routeMeta(), "iframeSrc") != null
                            || "LINK".equals(menu.type()) && stringMeta(menu.routeMeta(), "link") != null)
                    || !(superAdmin || granted.contains(menu.id()))) {
                continue;
            }
            Set<Long> lineage = activeLineage(menu, byId);
            if (lineage != null) selected.addAll(lineage);
        }
        Map<Long, MenuRouteVO> routes = new LinkedHashMap<>();
        all.stream()
                .filter(menu -> selected.contains(menu.id()) && !"BUTTON".equals(menu.type()))
                .sorted(Comparator.comparingInt(Menu::sortOrder).thenComparing(Menu::id))
                .forEach(menu -> routes.put(
                        menu.id(),
                        new MenuRouteVO(
                                menu.name(),
                                menu.path(),
                                "MENU".equals(menu.type())
                                        ? (superAdmin || granted.contains(menu.id()) ? menu.component() : null)
                                        : "CATALOG".equals(menu.type()) ? null : "IFrameView",
                                menu.redirect(),
                                new MenuMetaVO(
                                        menu.title(),
                                        menu.icon(),
                                        menu.sortOrder(),
                                        stringMeta(menu.routeMeta(), "iframeSrc"),
                                        stringMeta(menu.routeMeta(), "link"),
                                        stringMeta(menu.routeMeta(), "activeIcon"),
                                        stringMeta(menu.routeMeta(), "badgeType"),
                                        stringMeta(menu.routeMeta(), "badge"),
                                        stringMeta(menu.routeMeta(), "badgeVariants"),
                                        booleanMeta(menu.routeMeta(), "keepAlive"),
                                        booleanMeta(menu.routeMeta(), "affixTab"),
                                        booleanMeta(menu.routeMeta(), "hideInMenu"),
                                        booleanMeta(menu.routeMeta(), "hideChildrenInMenu"),
                                        booleanMeta(menu.routeMeta(), "hideInBreadcrumb"),
                                        booleanMeta(menu.routeMeta(), "hideInTab")),
                                new ArrayList<>())));
        List<MenuRouteVO> roots = new ArrayList<>();
        for (Menu menu : all) {
            MenuRouteVO route = routes.get(menu.id());
            if (route == null) {
                continue;
            }
            MenuRouteVO parent = menu.parentId() == null ? null : routes.get(menu.parentId());
            if (parent == null) {
                roots.add(route);
            } else {
                parent.children().add(route);
            }
        }
        Comparator<MenuRouteVO> order =
                Comparator.comparingInt(route -> route.meta().order());
        sortRoutes(roots, order);
        return roots;
    }

    private Set<Long> activeLineage(Menu menu, Map<Long, Menu> byId) {
        Set<Long> lineage = new HashSet<>();
        Menu cursor = menu;
        while (cursor != null) {
            if (!lineage.add(cursor.id())) return null;
            if (cursor.parentId() == null) return lineage;
            cursor = byId.get(cursor.parentId());
        }
        return null;
    }

    private void sortRoutes(List<MenuRouteVO> routes, Comparator<MenuRouteVO> order) {
        routes.sort(order);
        routes.forEach(route -> sortRoutes(route.children(), order));
    }

    private boolean routeContains(List<MenuRouteVO> routes, String path) {
        return routes.stream()
                .anyMatch(
                        route -> (route.component() != null && route.meta().link() == null && path.equals(route.path()))
                                || routeContains(route.children(), path));
    }

    private String firstPagePath(List<MenuRouteVO> routes) {
        for (MenuRouteVO route : routes) {
            if (route.component() != null && route.meta().link() == null) {
                return route.path();
            }
            String child = firstPagePath(route.children());
            if (!"/".equals(child)) {
                return child;
            }
        }
        return "/";
    }

    private List<String> roles(long userId) {
        return access.roleCodes(userId);
    }

    private Set<Long> grantedMenuIds(long userId) {
        return new HashSet<>(access.grantedMenuIds(userId));
    }

    private Set<Long> grantedPermissionIds(long userId) {
        return new HashSet<>(access.grantedPermissionIds(userId));
    }

    private List<Menu> menuRows() {
        return access.activeMenus().stream()
                .map(row -> new Menu(
                        row.getId(),
                        row.getParentId(),
                        row.getMenuType(),
                        row.getRouteName(),
                        row.getRoutePath(),
                        row.getComponent(),
                        row.getRedirectPath(),
                        row.getAuthCode(),
                        row.getTitleKey(),
                        row.getIcon(),
                        row.getSortOrder(),
                        routeMeta(row.getRouteMeta())))
                .toList();
    }

    private Map<?, ?> routeMeta(String value) {
        if (value == null) return Map.of();
        try {
            return json.readValue(value, Map.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored menu metadata", exception);
        }
    }

    private String stringMeta(Map<?, ?> meta, String key) {
        Object value = meta.get(key);
        return value instanceof String text ? text : null;
    }

    private Boolean booleanMeta(Map<?, ?> meta, String key) {
        Object value = meta.get(key);
        return value instanceof Boolean flag ? flag : null;
    }

    private record Menu(
            long id,
            Long parentId,
            String type,
            String name,
            String path,
            String component,
            String redirect,
            String authCode,
            String title,
            String icon,
            int sortOrder,
            Map<?, ?> routeMeta) {}
}
