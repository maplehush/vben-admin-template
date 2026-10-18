package com.maplehush.admin.modules.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.auth.service.BundledPageComponents;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import com.maplehush.admin.modules.system.menu.mapper.SysMenuMapper;
import com.maplehush.admin.modules.system.menu.service.ISysMenuService;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/** 维护菜单及按钮资源，校验层级、路由和授权引用。 */
@Service
public class SysMenuServiceImpl implements ISysMenuService {
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_.-]{3,128}");
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9:_-]{3,128}");
    private static final Pattern ROUTE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]{1,127}");
    private static final Set<String> TYPES = Set.of("CATALOG", "MENU", "BUTTON", "EMBEDDED", "LINK");
    private static final Set<String> CORE_KEYS =
            Set.of("menu.system", "menu.system.menu", "menu.system.permission", "menu.system.dict");
    private final SysMenuMapper menus;
    private final IManagementPermissionService permissions;
    private final ObjectMapper json;
    private final IOperationAuditService audit;
    private final TransactionTemplate transaction;

    public SysMenuServiceImpl(
            SysMenuMapper menus,
            IManagementPermissionService permissions,
            ObjectMapper json,
            IOperationAuditService audit,
            PlatformTransactionManager transactionManager) {
        this.menus = menus;
        this.permissions = permissions;
        this.json = json;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public long create(long actor, String type, String key, String authCode, MenuData data) {
        permissions.requireSuperAdmin(actor);
        if (type == null
                || !TYPES.contains(type)
                || key == null
                || !KEY.matcher(key).matches()
                || data == null) throw ApiException.invalidRequest();
        validate(type, authCode, data);
        try {
            return transaction.execute(tx -> {
                ensureParent(type, data.parentId());
                ensureUniqueRoutePath(data.routePath(), null);
                if (authCode != null) lockPermissionNamespace();
                ensureUniqueAuthCode(authCode);
                SysMenu menu = entity(null, type, key, authCode, data);
                menus.insert(menu);
                long id = menu.getId();
                audit.record(actor, "system:menu:create", AuditTargetType.MENU, id);
                return id;
            });
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void update(long actor, long id, MenuData data) {
        permissions.requireSuperAdmin(actor);
        if (data == null) throw ApiException.invalidRequest();
        try {
            transaction.executeWithoutResult(tx -> {
                SysMenu current = lock(id);
                if (CORE_KEYS.contains(current.getMenuKey())) {
                    // Core routes must remain reachable; only their presentation can change.
                    if (data.titleKey() == null
                            || data.titleKey().isBlank()
                            || data.titleKey().length() > 255
                            || data.icon() != null && data.icon().length() > 128) {
                        throw ApiException.invalidRequest();
                    }
                    menus.updateCore(id, data.titleKey().trim(), data.icon(), data.sortOrder());
                    audit.record(actor, "system:menu:update", AuditTargetType.MENU, id);
                    return;
                }
                if (protectedKey(current.getMenuKey())) throw ApiException.forbidden();
                validate(current.getMenuType(), current.getAuthCode(), data);
                if (data.parentId() != null && data.parentId() == id) throw ApiException.conflict();
                ensureParent(current.getMenuType(), data.parentId());
                if (data.parentId() != null && !data.parentId().equals(current.getParentId())) {
                    ensureNotDescendant(id, data.parentId());
                }
                ensureUniqueRoutePath(data.routePath(), id);
                if (current.getStatus() == 1
                        && SystemStatus.databaseValue(data.status()) == 0
                        && menus.selectCount(new LambdaQueryWrapper<SysMenu>()
                                        .eq(SysMenu::getParentId, id)
                                        .eq(SysMenu::getStatus, 1))
                                > 0) {
                    throw ApiException.conflict();
                }
                menus.updateDetails(
                        entity(id, current.getMenuType(), current.getMenuKey(), current.getAuthCode(), data));
                audit.record(actor, "system:menu:update", AuditTargetType.MENU, id);
            });
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void delete(long actor, long id) {
        permissions.requireSuperAdmin(actor);
        try {
            transaction.executeWithoutResult(tx -> {
                SysMenu current = lock(id);
                if (protectedKey(current.getMenuKey())) throw ApiException.forbidden();
                if (menus.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id)) > 0
                        || menus.countRoleReferences(id) > 0
                        || menus.countPermissionReferences(id) > 0) {
                    throw ApiException.conflict();
                }
                menus.deleteById(id);
                audit.record(actor, "system:menu:delete", AuditTargetType.MENU, id);
            });
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
    }

    private SysMenu lock(long id) {
        SysMenu row = menus.lock(id);
        if (row == null) throw ApiException.notFound();
        return row;
    }

    private boolean protectedKey(String key) {
        return CORE_KEYS.contains(key)
                || key.startsWith("perm.system.menu.")
                || key.startsWith("perm.system.permission.");
    }

    // Both code registries share this row lock so concurrent inserts cannot claim the same code.
    private void lockPermissionNamespace() {
        menus.lockPermissionNamespace();
    }

    private void validate(String type, String authCode, MenuData data) {
        if (data.titleKey() == null
                || data.titleKey().isBlank()
                || data.titleKey().length() > 255
                || data.icon() != null && data.icon().length() > 128
                || data.routeMeta() != null && !validMeta(data.routeMeta())) {
            throw ApiException.invalidRequest();
        }
        SystemStatus.databaseValue(data.status());
        if (!"BUTTON".equals(type)) {
            if (authCode != null
                    || data.routeName() == null
                    || !ROUTE_NAME.matcher(data.routeName()).matches()
                    || !validPath(data.routePath())
                    || data.redirectPath() != null && !validPath(data.redirectPath())) {
                throw ApiException.invalidRequest();
            }
            if ("MENU".equals(type) != BundledPageComponents.contains(data.component())) {
                throw ApiException.invalidRequest();
            }
            if (!"MENU".equals(type) && data.component() != null) {
                throw ApiException.invalidRequest();
            }
            boolean hasFrame = data.routeMeta() != null && data.routeMeta().containsKey("iframeSrc");
            boolean hasLink = data.routeMeta() != null && data.routeMeta().containsKey("link");
            if ("EMBEDDED".equals(type)
                    ? !validExternalUrl(data.routeMeta(), "iframeSrc") || hasLink
                    : "LINK".equals(type)
                            ? !validExternalUrl(data.routeMeta(), "link") || hasFrame
                            : hasFrame || hasLink) {
                throw ApiException.invalidRequest();
            }
        } else if ("BUTTON".equals(type)) {
            if (authCode == null
                    || !CODE.matcher(authCode).matches()
                    || data.routeName() != null
                    || data.routePath() != null
                    || data.component() != null
                    || data.redirectPath() != null
                    || data.routeMeta() != null
                            && (data.routeMeta().containsKey("iframeSrc")
                                    || data.routeMeta().containsKey("link"))) {
                throw ApiException.invalidRequest();
            }
        } else {
            throw ApiException.invalidRequest();
        }
    }

    private boolean validMeta(Map<String, Object> meta) {
        if (!Set.of(
                        "title",
                        "icon",
                        "order",
                        "iframeSrc",
                        "link",
                        "activeIcon",
                        "badgeType",
                        "badge",
                        "badgeVariants",
                        "keepAlive",
                        "affixTab",
                        "hideInMenu",
                        "hideChildrenInMenu",
                        "hideInBreadcrumb",
                        "hideInTab")
                .containsAll(meta.keySet())) return false;
        for (var item : meta.entrySet()) {
            String key = item.getKey();
            Object value = item.getValue();
            if ("order".equals(key)) {
                if (!(value instanceof Integer)) return false;
            } else if (Set.of(
                            "keepAlive",
                            "affixTab",
                            "hideInMenu",
                            "hideChildrenInMenu",
                            "hideInBreadcrumb",
                            "hideInTab")
                    .contains(key)) {
                if (!(value instanceof Boolean)) return false;
            } else if ("badgeType".equals(key)) {
                if (value == null || !Set.of("dot", "normal").contains(value)) return false;
            } else if ("badgeVariants".equals(key)) {
                if (value == null
                        || !Set.of("default", "destructive", "primary", "success", "warning")
                                .contains(value)) return false;
            } else if (!(value instanceof String text)
                    || text.length() > ("iframeSrc".equals(key) || "link".equals(key) ? 2048 : 255)) {
                return false;
            }
        }
        if ("normal".equals(meta.get("badgeType")) && (!(meta.get("badge") instanceof String badge) || badge.isBlank()))
            return false;
        return true;
    }

    private boolean validExternalUrl(Map<String, Object> meta, String key) {
        if (meta == null || !(meta.get(key) instanceof String value) || value.length() > 2048) return false;
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean validPath(String path) {
        return path != null
                && path.length() <= 255
                && path.startsWith("/")
                && !path.startsWith("//")
                && !path.contains("..")
                && !path.contains("?")
                && !path.contains("#");
    }

    private void ensureParent(String type, Long parentId) {
        if (parentId == null) {
            if (!"CATALOG".equals(type)) throw ApiException.invalidRequest();
            return;
        }
        SysMenu parent = menus.lockParent(parentId);
        if (parent == null || parent.getStatus() != 1) throw ApiException.conflict();
        String parentType = parent.getMenuType();
        if (!"CATALOG".equals(parentType) && !"MENU".equals(parentType)) {
            throw ApiException.invalidRequest();
        }
    }

    private void ensureNotDescendant(long movingId, long parentId) {
        Set<Long> seen = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null) {
            if (cursor == movingId || !seen.add(cursor)) throw ApiException.conflict();
            cursor = menus.parentOf(cursor);
        }
    }

    private void ensureUniqueRoutePath(String path, Long exceptId) {
        if (path == null) return;
        if (menus.selectCount(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getRoutePath, path)
                        .ne(exceptId != null, SysMenu::getId, exceptId))
                > 0) throw ApiException.conflict();
    }

    private void ensureUniqueAuthCode(String code) {
        if (code == null) return;
        if (menus.countPermissionCode(code) > 0) throw ApiException.conflict();
    }

    private String metaJson(Map<String, Object> meta) {
        return meta == null ? null : json.writeValueAsString(meta);
    }

    private SysMenu entity(Long id, String type, String key, String authCode, MenuData data) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(data.parentId());
        menu.setMenuType(type);
        menu.setMenuKey(key);
        menu.setRouteName(data.routeName());
        menu.setRoutePath(data.routePath());
        menu.setComponent(data.component());
        menu.setRedirectPath(data.redirectPath());
        menu.setAuthCode(authCode);
        menu.setTitleKey(data.titleKey().trim());
        menu.setIcon(data.icon());
        menu.setSortOrder(data.sortOrder());
        menu.setRouteMeta(metaJson(data.routeMeta()));
        menu.setStatus(SystemStatus.databaseValue(data.status()));
        return menu;
    }
}
