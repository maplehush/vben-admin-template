package com.maplehush.admin.modules.system.auth.service;

import java.util.Set;

/** Keep this list aligned with views bundled by the web-antdv-next application. */
public final class BundledPageComponents {
    private static final Set<String> PATHS = Set.of(
            "/dashboard/workspace/index",
            "/system/user/list",
            "/system/role/list",
            "/system/dept/list",
            "/system/dict/list",
            "/system/menu/list",
            "/system/permission/list",
            "/system/session/list",
            "/system/log/login/list",
            "/system/log/operation/list");

    private BundledPageComponents() {}

    public static boolean contains(String path) {
        return path != null && PATHS.contains(path);
    }
}
