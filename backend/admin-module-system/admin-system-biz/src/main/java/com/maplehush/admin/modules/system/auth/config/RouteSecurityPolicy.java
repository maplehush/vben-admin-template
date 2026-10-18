package com.maplehush.admin.modules.system.auth.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpMethod;

/** 控制器路由的认证入口清单，业务权限仍由服务层校验 */
public final class RouteSecurityPolicy {
    private RouteSecurityPolicy() {}

    public enum Access {
        PUBLIC,
        AUTHENTICATED
    }

    public record Route(HttpMethod method, String path, Access access) {}

    public static final List<Route> ROUTES = routes();

    private static List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        add(routes, Access.PUBLIC, HttpMethod.GET, "/auth/captcha");
        add(routes, Access.PUBLIC, HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout");

        add(
                routes,
                Access.AUTHENTICATED,
                HttpMethod.GET,
                "/user/info",
                "/auth/codes",
                "/menu/all",
                "/auth/sessions",
                "/system/sessions",
                "/system/login-logs",
                "/system/operation-logs",
                "/system/users",
                "/system/users/{userId}",
                "/system/roles",
                "/system/roles/{roleId}",
                "/system/roles/{roleId}/menus",
                "/system/roles/{roleId}/permissions",
                "/system/roles/{roleId}/data-scope",
                "/system/depts/tree",
                "/system/depts/{deptId}",
                "/system/dicts",
                "/system/dicts/{dictId}",
                "/system/dicts/{dictId}/items",
                "/system/dict-options/{code}",
                "/system/menus/tree",
                "/system/menus/{menuId}",
                "/system/permissions",
                "/system/permissions/{permissionId}");
        add(
                routes,
                Access.AUTHENTICATED,
                HttpMethod.DELETE,
                "/auth/sessions/{sessionId}",
                "/system/sessions/{sessionId}",
                "/system/roles/{roleId}",
                "/system/depts/{deptId}",
                "/system/menus/{menuId}",
                "/system/permissions/{permissionId}",
                "/system/dicts/{dictId}",
                "/system/dicts/{dictId}/items/{itemId}");
        add(
                routes,
                Access.AUTHENTICATED,
                HttpMethod.POST,
                "/system/roles",
                "/system/depts",
                "/system/menus",
                "/system/permissions",
                "/system/dicts",
                "/system/dicts/{dictId}/items",
                "/upload",
                "/system/users",
                "/system/users/{userId}/reset-password");
        add(
                routes,
                Access.AUTHENTICATED,
                HttpMethod.PUT,
                "/user/password",
                "/user/profile",
                "/system/roles/{roleId}",
                "/system/roles/{roleId}/menus",
                "/system/roles/{roleId}/permissions",
                "/system/roles/{roleId}/data-scope",
                "/system/depts/{deptId}",
                "/system/menus/{menuId}",
                "/system/permissions/{permissionId}",
                "/system/dicts/{dictId}",
                "/system/dicts/{dictId}/items/{itemId}",
                "/system/users/{userId}",
                "/system/users/{userId}/status",
                "/system/users/{userId}/roles");
        return List.copyOf(routes);
    }

    private static void add(List<Route> routes, Access access, HttpMethod method, String... paths) {
        for (String path : paths) {
            routes.add(new Route(method, path, access));
        }
    }
}
