package com.maplehush.admin.modules.system.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.menu.mapper.SysMenuMapper;
import com.maplehush.admin.modules.system.menu.service.impl.SysMenuServiceImpl;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

class SysMenuManagementServiceTest {
    private JdbcTemplate jdbc;
    private ISysMenuService menus;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:menu_write_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, menu_id BIGINT, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE sys_menu (id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT,
                  menu_type VARCHAR(16), menu_key VARCHAR(128) UNIQUE,
                  route_name VARCHAR(128) UNIQUE, route_path VARCHAR(255), component VARCHAR(255),
                  redirect_path VARCHAR(255), auth_code VARCHAR(128) UNIQUE,
                  title_key VARCHAR(255), icon VARCHAR(128), sort_order INT,
                  route_meta VARCHAR(1000), status TINYINT)
                """);
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1), (2,'reader',1)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update("""
                INSERT INTO sys_menu (id,parent_id,menu_type,menu_key,route_name,route_path,
                    component,auth_code,title_key,sort_order,status) VALUES
                (10,NULL,'CATALOG','menu.system','System','/system',NULL,NULL,'System',0,1),
                (11,10,'MENU','menu.system.menu','Resource','/system/resource','/system/menu/list',NULL,'Resource',1,1),
                (12,11,'BUTTON','perm.system.menu.update',NULL,NULL,NULL,'system:menu:update','Update',1,1),
                (20,10,'MENU','menu.other','Other','/other','/dashboard/workspace/index',NULL,'Other',2,1),
                (21,20,'BUTTON','perm.other.create',NULL,NULL,NULL,'other:create','Create',1,1),
                (30,NULL,'CATALOG','menu.parent','Parent','/parent',NULL,NULL,'Parent',3,1),
                (31,30,'CATALOG','menu.child','Child','/child',NULL,NULL,'Child',1,1)
                """);
        menus = new SysMenuServiceImpl(
                TestMapperFactory.create(source).getMapper(SysMenuMapper.class),
                new ManagementPermissionServiceImpl(
                        TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class)),
                new ObjectMapper(),
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class)),
                new DataSourceTransactionManager(source));
    }

    private ISysMenuService.MenuData data(
            Long parent, String name, String path, String component, String title, String status) {
        return new ISysMenuService.MenuData(parent, name, path, component, null, title, null, 0, null, status);
    }

    @Test
    void createsOnlyValidTypedResourcesAndRejectsUnknownComponent() {
        long menu = menus.create(
                1, "MENU", "menu.new", null, data(10L, "New", "/new", "/system/user/list", "New", "ENABLED"));
        assertEquals("MENU", jdbc.queryForObject("SELECT menu_type FROM sys_menu WHERE id = ?", String.class, menu));
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "MENU",
                                        "menu.bad",
                                        null,
                                        data(10L, "Bad", "/bad", "/unknown/page", "Bad", "ENABLED")))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "BUTTON",
                                        "button.bad",
                                        "bad:code",
                                        data(10L, "Illegal", "/illegal", null, "Bad", "ENABLED")))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "MENU",
                                        "menu.meta",
                                        null,
                                        new ISysMenuService.MenuData(
                                                10L,
                                                "Meta",
                                                "/meta",
                                                "/system/user/list",
                                                null,
                                                "Meta",
                                                null,
                                                0,
                                                Map.of("script", "alert(1)"),
                                                "ENABLED")))
                        .code());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        2,
                                        "CATALOG",
                                        "no.access",
                                        null,
                                        data(null, "No", "/no", null, "No", "ENABLED")))
                        .code());
    }

    @Test
    void pageMenuCanContainNestedDirectoriesAndPages() {
        long directory = menus.create(
                1,
                "CATALOG",
                "menu.nested.directory",
                null,
                data(11L, "NestedDirectory", "/system/resource/nested", null, "Nested directory", "ENABLED"));
        assertEquals(11L, jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id = ?", Long.class, directory));

        long page = menus.create(
                1,
                "MENU",
                "menu.nested.page",
                null,
                data(
                        directory,
                        "NestedPage",
                        "/system/resource/nested/page",
                        "/system/user/list",
                        "Nested page",
                        "ENABLED"));
        assertEquals(directory, jdbc.queryForObject("SELECT parent_id FROM sys_menu WHERE id = ?", Long.class, page));
    }

    @Test
    void embeddedAndLinkRequireSafeUrlsAndDoNotAcceptPageComponents() {
        var embedded = new ISysMenuService.MenuData(
                10L,
                "EmbeddedDocs",
                "/embedded-docs",
                null,
                null,
                "文档",
                null,
                0,
                Map.of("iframeSrc", "https://example.com/docs"),
                "ENABLED");
        long embeddedId = menus.create(1, "EMBEDDED", "menu.embedded.docs", null, embedded);
        assertEquals(
                "EMBEDDED",
                jdbc.queryForObject("SELECT menu_type FROM sys_menu WHERE id = ?", String.class, embeddedId));

        var link = new ISysMenuService.MenuData(
                10L,
                "ExternalDocs",
                "/external-docs",
                null,
                null,
                "外部文档",
                null,
                0,
                Map.of("link", "https://example.com/docs"),
                "ENABLED");
        long linkId = menus.create(1, "LINK", "menu.external.docs", null, link);
        assertEquals("LINK", jdbc.queryForObject("SELECT menu_type FROM sys_menu WHERE id = ?", String.class, linkId));

        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "LINK",
                                        "menu.bad.link",
                                        null,
                                        new ISysMenuService.MenuData(
                                                10L,
                                                "BadLink",
                                                "/bad-link",
                                                null,
                                                null,
                                                "Bad",
                                                null,
                                                0,
                                                Map.of("link", "javascript:alert(1)"),
                                                "ENABLED")))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "EMBEDDED",
                                        "menu.bad.frame",
                                        null,
                                        new ISysMenuService.MenuData(
                                                10L,
                                                "BadFrame",
                                                "/bad-frame",
                                                "/system/user/list",
                                                null,
                                                "Bad",
                                                null,
                                                0,
                                                Map.of("iframeSrc", "https://example.com"),
                                                "ENABLED")))
                        .code());
    }

    @Test
    void vbenBadgeAndVisibilityMetadataIsValidatedAndStored() {
        var meta = Map.<String, Object>of(
                "badgeType",
                "normal",
                "badge",
                "New",
                "badgeVariants",
                "primary",
                "activeIcon",
                "mdi:menu-open",
                "hideInMenu",
                true,
                "affixTab",
                false);
        long id = menus.create(
                1,
                "MENU",
                "menu.badged",
                null,
                new ISysMenuService.MenuData(
                        10L, "Badged", "/badged", "/system/user/list", null, "Badged", null, 0, meta, "ENABLED"));
        String stored = jdbc.queryForObject("SELECT route_meta FROM sys_menu WHERE id = ?", String.class, id);
        assertEquals(true, stored.contains("\"badgeType\":\"normal\""));
        assertEquals(true, stored.contains("\"hideInMenu\":true"));
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "MENU",
                                        "menu.badge.invalid",
                                        null,
                                        new ISysMenuService.MenuData(
                                                10L,
                                                "InvalidBadge",
                                                "/invalid-badge",
                                                "/system/user/list",
                                                null,
                                                "Invalid",
                                                null,
                                                0,
                                                Map.of("badgeType", "normal"),
                                                "ENABLED")))
                        .code());
    }

    @Test
    void movingToDescendantAndDisablingActiveParentAreRejected() {
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> menus.update(1, 30, data(31L, "Parent", "/parent", null, "Parent", "ENABLED")))
                        .code());
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> menus.update(
                                        1,
                                        20,
                                        data(
                                                10L,
                                                "Other",
                                                "/other",
                                                "/dashboard/workspace/index",
                                                "Other",
                                                "DISABLED")))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> menus.update(1, 21, data(10L, "Create", "/create", null, "Create", "ENABLED")))
                        .code());
    }

    @Test
    void deleteRequiresNoChildrenOrGrantsAndPreservesManagementEntry() {
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> menus.delete(1, 11)).code());
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> menus.delete(1, 20)).code());
        jdbc.update("INSERT INTO sys_role_menu VALUES (2,21)");
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> menus.delete(1, 21)).code());
        jdbc.update("DELETE FROM sys_role_menu WHERE menu_id = 21");
        menus.delete(1, 21);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE id = 21", Integer.class));
    }

    @Test
    void routePathAndAuthCodeMustBeUnique() {
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "MENU",
                                        "menu.duplicate",
                                        null,
                                        data(10L, "Again", "/other", "/system/user/list", "Again", "ENABLED")))
                        .code());
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> menus.create(
                                        1,
                                        "BUTTON",
                                        "button.duplicate",
                                        "other:create",
                                        data(20L, null, null, null, "Again", "ENABLED")))
                        .code());
    }

    @Test
    void updatePreservesImmutableIdentityAndChangesEditableFields() {
        menus.update(
                1,
                20,
                new ISysMenuService.MenuData(
                        10L,
                        "Other",
                        "/other-renamed",
                        "/dashboard/workspace/index",
                        null,
                        "Renamed",
                        "lucide:box",
                        5,
                        Map.of("order", 5),
                        "ENABLED"));
        assertEquals(
                "/other-renamed", jdbc.queryForObject("SELECT route_path FROM sys_menu WHERE id = 20", String.class));
        assertEquals("menu.other", jdbc.queryForObject("SELECT menu_key FROM sys_menu WHERE id = 20", String.class));
        assertEquals(
                "{\"order\":5}", jdbc.queryForObject("SELECT route_meta FROM sys_menu WHERE id = 20", String.class));
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> menus.update(1, 12, data(11L, null, null, null, "Changed", "ENABLED")))
                        .code());
    }

    @Test
    void coreMenuCanChangePresentationWithoutChangingItsRoute() {
        menus.update(
                1,
                10,
                new ISysMenuService.MenuData(
                        30L, "Broken", "/broken", null, null, "系统设置", "lucide:settings", 7, null, "DISABLED"));
        assertEquals("系统设置", jdbc.queryForObject("SELECT title_key FROM sys_menu WHERE id = 10", String.class));
        assertEquals("lucide:settings", jdbc.queryForObject("SELECT icon FROM sys_menu WHERE id = 10", String.class));
        assertEquals(7, jdbc.queryForObject("SELECT sort_order FROM sys_menu WHERE id = 10", Integer.class));
        assertEquals("/system", jdbc.queryForObject("SELECT route_path FROM sys_menu WHERE id = 10", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM sys_menu WHERE id = 10", Integer.class));
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> menus.update(1, 12, data(11L, null, null, null, "Changed", "ENABLED")))
                        .code());
    }

    @Test
    void menuLifecycleRejectsOrdinaryRoleAtEveryWriteStep() {
        var initial = data(10L, "AcceptanceMenu", "/acceptance-menu", "/system/user/list", "Acceptance", "ENABLED");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> menus.create(2, "MENU", "menu.acceptance", null, initial))
                        .code());

        long id = menus.create(1, "MENU", "menu.acceptance", null, initial);
        var changed = data(
                10L,
                "AcceptanceMenu",
                "/acceptance-menu-updated",
                "/system/user/list",
                "Acceptance Updated",
                "ENABLED");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> menus.update(2, id, changed))
                        .code());
        menus.update(1, id, changed);
        assertEquals(
                "/acceptance-menu-updated",
                jdbc.queryForObject("SELECT route_path FROM sys_menu WHERE id = ?", String.class, id));

        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> menus.delete(2, id)).code());
        menus.delete(1, id);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE id = ?", Integer.class, id));
    }
}
