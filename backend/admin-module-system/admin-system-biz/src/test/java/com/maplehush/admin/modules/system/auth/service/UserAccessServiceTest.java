package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.modules.system.auth.mapper.UserAccessMapper;
import com.maplehush.admin.modules.system.auth.service.impl.UserAccessServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

class UserAccessServiceTest {
    private JdbcTemplate jdbc;
    private IUserAccessService access;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:access_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(64),
                  real_name VARCHAR(128), email VARCHAR(255), phone VARCHAR(32),
                  avatar_url VARCHAR(255), home_path VARCHAR(255),
                  must_change_password TINYINT)
                """);
        jdbc.execute("CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT,
                  menu_type VARCHAR(16), route_name VARCHAR(128), route_path VARCHAR(255),
                  component VARCHAR(255), redirect_path VARCHAR(255), auth_code VARCHAR(128),
                  title_key VARCHAR(255), icon VARCHAR(128), sort_order INT, status TINYINT, route_meta VARCHAR(4096))
                """);
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.update("INSERT INTO sys_user VALUES (1, 'admin', 'Admin', 'admin@example.com', NULL, NULL, NULL, 0)");
        jdbc.update("INSERT INTO sys_role VALUES (2, 'editor', 1)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1, 2)");
        jdbc.update(
                "INSERT INTO sys_menu VALUES (10, NULL, 'CATALOG', 'Dashboard', '/dashboard', NULL, '/dashboard/workspace', NULL, 'dashboard', NULL, 0, 1, NULL)");
        jdbc.update(
                "INSERT INTO sys_menu VALUES (11, 10, 'MENU', 'Workspace', '/dashboard/workspace', '/dashboard/workspace/index', NULL, NULL, 'workspace', NULL, 1, 1, NULL)");
        jdbc.update(
                "INSERT INTO sys_menu VALUES (12, 11, 'BUTTON', NULL, NULL, NULL, NULL, 'system:user:create', 'create', NULL, 0, 1, NULL)");
        jdbc.update("INSERT INTO sys_role_menu VALUES (2, 11)");
        jdbc.update("INSERT INTO sys_role_menu VALUES (2, 12)");
        access = new UserAccessServiceImpl(
                TestMapperFactory.create(source).getMapper(UserAccessMapper.class), new ObjectMapper());
    }

    @Test
    void grantedPageIncludesParentAndGrantedCode() {
        assertEquals("1", access.info(1).userId());
        assertEquals("admin@example.com", access.info(1).email());
        assertEquals("/dashboard/workspace", access.info(1).homePath());
        assertEquals(1, access.menus(1).size());
        assertEquals("Dashboard", access.menus(1).getFirst().name());
        assertEquals(
                "Workspace", access.menus(1).getFirst().children().getFirst().name());
        assertEquals("system:user:create", access.codes(1).getFirst());
    }

    @Test
    void implicitPageParentDoesNotGrantAccessToItsPage() {
        jdbc.update("""
                INSERT INTO sys_menu VALUES
                (13, 11, 'CATALOG', 'Nested', '/dashboard/workspace/nested', NULL, NULL,
                 NULL, 'nested', NULL, 0, 1, NULL),
                (14, 13, 'MENU', 'NestedPage', '/dashboard/workspace/nested/page',
                 '/system/user/list', NULL, NULL, 'nested page', NULL, 0, 1, NULL)
                """);
        jdbc.update("DELETE FROM sys_role_menu WHERE menu_id = 11");
        jdbc.update("INSERT INTO sys_role_menu VALUES (2, 14)");

        var parent = access.menus(1).getFirst().children().getFirst();
        assertEquals("Workspace", parent.name());
        assertEquals(null, parent.component());
        assertEquals(
                "NestedPage", parent.children().getFirst().children().getFirst().name());
    }

    @Test
    void disabledRoleRemovesMenuAndCode() {
        jdbc.update("UPDATE sys_role SET status = 0 WHERE id = 2");
        assertTrue(access.codes(1).isEmpty());
        assertTrue(access.menus(1).isEmpty());
        assertTrue(access.info(1).roles().isEmpty());
    }

    @Test
    void unbundledPageIsNotReturned() {
        jdbc.update("UPDATE sys_menu SET component = '/system/unknown/list' WHERE id = 11");
        assertTrue(access.menus(1).isEmpty());
    }

    @Test
    void bundledSystemPageIsReturned() {
        jdbc.update("UPDATE sys_menu SET component = '/system/user/list' WHERE id = 11");
        assertEquals(
                "Workspace", access.menus(1).getFirst().children().getFirst().name());
        jdbc.update("UPDATE sys_menu SET component = '/system/dept/list' WHERE id = 11");
        assertEquals(
                "Workspace", access.menus(1).getFirst().children().getFirst().name());
        jdbc.update("UPDATE sys_menu SET component = '/system/menu/list' WHERE id = 11");
        assertEquals(
                "Workspace", access.menus(1).getFirst().children().getFirst().name());
        jdbc.update("UPDATE sys_menu SET component = '/system/session/list' WHERE id = 11");
        assertEquals(
                "Workspace", access.menus(1).getFirst().children().getFirst().name());
    }

    @Test
    void grantedDictionaryPageAppearsInLoginMenu() {
        jdbc.update("""
                INSERT INTO sys_menu VALUES
                (20, NULL, 'CATALOG', 'System', '/system', NULL, NULL, NULL,
                 'system.title', NULL, 0, 1, NULL),
                (21, 20, 'MENU', 'SystemDict', '/system/dict', '/system/dict/list', NULL, NULL,
                 'system.dict.title', 'carbon:dictionary', 55, 1, NULL)
                """);
        jdbc.update("INSERT INTO sys_role_menu VALUES (2, 21)");

        var system = access.menus(1).stream()
                .filter(menu -> "System".equals(menu.name()))
                .findFirst()
                .orElse(null);
        assertNotNull(system);
        assertEquals("SystemDict", system.children().getFirst().name());
        assertEquals("/system/dict/list", system.children().getFirst().component());
    }

    @Test
    void embeddedAndLinkRoutesExposeVbenMetadata() {
        jdbc.update("""
                INSERT INTO sys_menu VALUES
                (13, 10, 'EMBEDDED', 'EmbeddedDocs', '/embedded-docs', NULL, NULL,
                 NULL, 'Embedded', NULL, 2, 1, '{"iframeSrc":"https://example.com/embed"}'),
                (14, 10, 'LINK', 'ExternalDocs', '/external-docs', NULL, NULL,
                 NULL, 'External', NULL, 3, 1, '{"link":"https://example.com/docs"}')
                """);
        jdbc.update("INSERT INTO sys_role_menu VALUES (2,13), (2,14)");
        var children = access.menus(1).getFirst().children();
        assertEquals("IFrameView", children.get(1).component());
        assertEquals("https://example.com/embed", children.get(1).meta().iframeSrc());
        assertEquals("IFrameView", children.get(2).component());
        assertEquals("https://example.com/docs", children.get(2).meta().link());
    }

    @Test
    void vbenBadgeAndVisibilityMetadataReachesRoutes() {
        jdbc.update("""
                UPDATE sys_menu SET route_meta =
                  '{"activeIcon":"mdi:menu-open","badgeType":"normal","badge":"New",
                    "badgeVariants":"primary","keepAlive":true,"affixTab":true,
                    "hideInMenu":true,"hideChildrenInMenu":false,
                    "hideInBreadcrumb":true,"hideInTab":true}'
                WHERE id = 11
                """);
        var meta = access.menus(1).getFirst().children().getFirst().meta();
        assertEquals("New", meta.badge());
        assertEquals("primary", meta.badgeVariants());
        assertEquals("mdi:menu-open", meta.activeIcon());
        assertTrue(meta.keepAlive());
        assertTrue(meta.affixTab());
        assertTrue(meta.hideInMenu());
        assertTrue(meta.hideInBreadcrumb());
        assertTrue(meta.hideInTab());
    }

    @Test
    void disabledParentSuppressesChildMenu() {
        jdbc.update("UPDATE sys_menu SET status = 0 WHERE id = 10");
        assertTrue(access.menus(1).isEmpty());
    }

    @Test
    void disabledGrandparentSuppressesNestedMenu() {
        jdbc.update(
                "INSERT INTO sys_menu VALUES (8, NULL, 'CATALOG', 'Root', '/root', NULL, NULL, NULL, 'root', NULL, 0, 0, NULL)");
        jdbc.update("UPDATE sys_menu SET parent_id = 8 WHERE id = 10");
        assertTrue(access.menus(1).isEmpty());
    }
}
