package com.maplehush.admin.modules.system.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import com.maplehush.admin.modules.system.role.dto.RoleQueryDTO;
import com.maplehush.admin.modules.system.role.mapper.SysRoleMapper;
import com.maplehush.admin.modules.system.role.service.ISysRoleQueryService;
import com.maplehush.admin.modules.system.role.service.impl.SysRoleQueryServiceImpl;
import com.maplehush.admin.modules.system.user.dto.UserQueryDTO;
import com.maplehush.admin.modules.system.user.mapper.SysUserQueryMapper;
import com.maplehush.admin.modules.system.user.service.impl.SysUserQueryServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UserAndRoleQueryServiceTest {
    private JdbcTemplate jdbc;
    private ISysUserQueryService queries;
    private ISysRoleQueryService roleQueries;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:management_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, parent_id BIGINT, dept_name VARCHAR(128))");
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(64), real_name VARCHAR(128), remark VARCHAR(500),
                  email VARCHAR(255), phone VARCHAR(32), primary_dept_id BIGINT, status TINYINT,
                  avatar_url VARCHAR(1024), home_path VARCHAR(255),
                  create_time TIMESTAMP, update_time TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), role_name VARCHAR(128),
                  description VARCHAR(500), status TINYINT, data_scope_type VARCHAR(32),
                  create_time TIMESTAMP, update_time TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute(
                "CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, auth_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_dept_scope (role_id BIGINT, dept_id BIGINT)");
        jdbc.update("INSERT INTO sys_dept VALUES (10, NULL, 'Root'), (11, 10, 'Child'), (20, NULL, 'Elsewhere')");
        jdbc.update("""
                INSERT INTO sys_user VALUES
                (1, 'actor', 'Actor', NULL, NULL, NULL, 10, 1, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (2, 'child', 'Child', NULL, NULL, NULL, 11, 1, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (3, 'outside', 'Outside', NULL, NULL, NULL, 20, 1, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO sys_role VALUES
                (1, 'viewer', 'Viewer', NULL, 1, 'DEPT_AND_CHILDREN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                (2, 'all_other', 'All other', NULL, 1, 'ALL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("INSERT INTO sys_user_role VALUES (1, 1), (1, 2)");
        jdbc.update("""
                INSERT INTO sys_permission VALUES
                (1, 'system:user:list', 1), (2, 'system:user:view', 1),
                (3, 'system:role:list', 1), (4, 'system:role:view', 1)
                """);
        jdbc.update("INSERT INTO sys_role_permission VALUES (1, 1), (1, 2), (2, 3), (2, 4)");
        var permissions = new ManagementPermissionServiceImpl(
                TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class));
        var factory = TestMapperFactory.create(source);
        queries = new SysUserQueryServiceImpl(factory.getMapper(SysUserQueryMapper.class), permissions);
        roleQueries = new SysRoleQueryServiceImpl(factory.getMapper(SysRoleMapper.class), permissions);
    }

    @Test
    void userScopeOnlyUsesRolesGrantingCurrentCode() {
        var page = queries.users(1, userQuery(null));
        assertEquals(2, page.total());
        assertEquals("2", page.items().getFirst().id());
        assertEquals("11", page.items().getFirst().primaryDeptId());
        assertEquals("ENABLED", page.items().getFirst().status());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> queries.user(1, 3)).code());
        assertEquals("2", queries.user(1, 2).id());
        assertEquals("ENABLED", queries.user(1, 2).status());
    }

    @Test
    void userFiltersConvertOffsetCreationRangeToBeijingWithinScope() {
        jdbc.update("UPDATE sys_user SET remark = 'on-call', create_time = '2026-10-02 05:00:00' WHERE id = 2");
        jdbc.update("UPDATE sys_user SET remark = 'on-call', create_time = '2026-10-02 05:00:00' WHERE id = 3");
        var page = queries.users(
                1,
                new UserQueryDTO(
                        1,
                        20,
                        "child",
                        null,
                        null,
                        "ENABLED",
                        2L,
                        "on-call",
                        "2026-10-01T16:00:00Z",
                        "2026-10-02T16:00:00Z"));
        assertEquals(1, page.total());
        assertEquals("on-call", page.items().getFirst().remark());
        assertEquals(
                0,
                queries.users(1, new UserQueryDTO(1, 20, null, null, null, null, 3L, null, null, null))
                        .total());
    }

    @Test
    void roleQueriesNeedTheirOwnPermissionAndPage() {
        var page = roleQueries.roles(1, new RoleQueryDTO(1, 1, null, null, null, null, null, null));
        assertEquals(2, page.total());
        assertEquals(1, page.items().size());
        assertEquals("all_other", page.items().getFirst().roleCode());
        var nextPage = roleQueries.roles(1, new RoleQueryDTO(2, 1, null, null, null, null, null, null));
        assertEquals(2, nextPage.total());
        assertEquals("viewer", nextPage.items().getFirst().roleCode());
        assertEquals("viewer", roleQueries.role(1, 1).roleCode());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> roleQueries.roles(1, new RoleQueryDTO(0, 20, null, null, null, null, null, null)))
                        .code());
        jdbc.update("UPDATE sys_permission SET status = 0 WHERE id = 3");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> roleQueries.roles(1, roleQuery(null)))
                        .code());
    }

    @Test
    void roleFiltersConvertOffsetCreationRangeToBeijing() {
        jdbc.update("UPDATE sys_role SET description = 'operations', create_time = '2026-10-02 05:00:00' WHERE id = 1");
        jdbc.update("UPDATE sys_role SET description = 'finance', create_time = '2026-10-01 05:00:00' WHERE id = 2");
        var page = roleQueries.roles(
                1,
                new RoleQueryDTO(
                        1, 20, 1L, "View", "ENABLED", "opera", "2026-10-01T16:00:00Z", "2026-10-02T16:00:00Z"));
        assertEquals(1, page.total());
        assertEquals("1", page.items().getFirst().id());
        assertEquals(
                0,
                roleQueries
                        .roles(
                                1,
                                new RoleQueryDTO(
                                        1, 20, 1L, null, null, null, "2026-10-01T21:00:00Z", "2026-10-01T21:00:00Z"))
                        .total());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> roleQueries.roles(
                                        1, new RoleQueryDTO(1, 20, null, null, null, null, "invalid", null)))
                        .code());
    }

    @Test
    void roleNameSearchTreatsSqlWildcardsAsLiteralCharacters() {
        assertEquals(0, roleQueries.roles(1, roleQuery("_")).total());
        assertEquals(0, roleQueries.roles(1, roleQuery("%")).total());
    }

    @Test
    void superAdminSeesAllButMissingRecordIsNotFound() {
        jdbc.update(
                "INSERT INTO sys_role VALUES (3, 'super_admin', 'Super', NULL, 1, 'SELF', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1, 3)");
        assertEquals(3, queries.users(1, userQuery(null)).total());
        assertEquals(
                true, queries.users(1, userQuery("actor")).items().getFirst().systemAccount());
        assertEquals(false, queries.user(1, 2).systemAccount());
        assertEquals(
                40400,
                assertThrows(ApiException.class, () -> queries.user(1, 99)).code());
    }

    @Test
    void actorWithoutDepartmentStillSeesSelfAndDisabledGrantIsRejected() {
        jdbc.update("UPDATE sys_user SET primary_dept_id = NULL WHERE id = 1");
        assertEquals(1, queries.users(1, userQuery(null)).total());
        jdbc.update("UPDATE sys_role SET status = 0 WHERE id = 1");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> queries.users(1, userQuery(null)))
                        .code());
    }

    private UserQueryDTO userQuery(String username) {
        return new UserQueryDTO(1, 20, username, null, null, null, null, null, null, null);
    }

    private RoleQueryDTO roleQuery(String roleName) {
        return new RoleQueryDTO(1, 20, null, roleName, null, null, null, null);
    }
}
