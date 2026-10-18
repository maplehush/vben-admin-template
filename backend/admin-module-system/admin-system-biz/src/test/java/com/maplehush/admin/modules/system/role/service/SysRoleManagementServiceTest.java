package com.maplehush.admin.modules.system.role.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import com.maplehush.admin.modules.system.role.mapper.SysRoleMapper;
import com.maplehush.admin.modules.system.role.service.impl.SysRoleServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class SysRoleManagementServiceTest {
    private JdbcTemplate jdbc;
    private ISysRoleService roles;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:role_write_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY)");
        jdbc.execute("""
                CREATE TABLE sys_role (id BIGINT AUTO_INCREMENT PRIMARY KEY, role_code VARCHAR(64) UNIQUE,
                  role_name VARCHAR(128), description VARCHAR(500), status TINYINT,
                  data_scope_type VARCHAR(32), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, status TINYINT, auth_code VARCHAR(128))");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT, PRIMARY KEY(role_id, menu_id))");
        jdbc.execute("CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, status TINYINT)");
        jdbc.execute(
                "CREATE TABLE sys_role_dept_scope (role_id BIGINT, dept_id BIGINT, PRIMARY KEY(role_id, dept_id))");
        jdbc.execute("""
                CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64),
                  target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))
                """);
        jdbc.update("INSERT INTO sys_user VALUES (1), (2)");
        jdbc.update("INSERT INTO sys_role (id,role_code,role_name,status,data_scope_type) VALUES "
                + "(1,'super_admin','Super',1,'ALL'), (10,'editor','Editor',1,'SELF')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,10)");
        jdbc.update("INSERT INTO sys_menu VALUES (100,1,'a'), (101,1,'b'), (102,0,'c')");
        jdbc.update("INSERT INTO sys_role_menu VALUES (10,100)");
        jdbc.update("INSERT INTO sys_dept VALUES (20,1), (21,0)");
        roles = new SysRoleServiceImpl(
                TestMapperFactory.create(source).getMapper(SysRoleMapper.class),
                new ManagementPermissionServiceImpl(
                        TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class)),
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class)),
                new DataSourceTransactionManager(source));
    }

    @Test
    void onlyActiveSuperAdminCanCreateAndCodeMustBeUnique() {
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> roles.create(2, "reader", "Reader", null, "ENABLED"))
                        .code());
        long id = roles.create(1, "reader", "Reader", "Read only", "ENABLED");
        assertEquals(
                "SELF", jdbc.queryForObject("SELECT data_scope_type FROM sys_role WHERE id = ?", String.class, id));
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> roles.create(1, "reader", "Again", null, "ENABLED"))
                        .code());
    }

    @Test
    void successfulWriteAuditsRequestIdentityAndSource() {
        var request = new MockHttpServletRequest();
        request.setAttribute("requestId", "req-123");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        long id = roles.create(1, "auditor", "Auditor", null, "ENABLED");
        assertEquals(
                "req-123",
                jdbc.queryForObject(
                        "SELECT request_id FROM sys_operation_log WHERE target_id = ?",
                        String.class,
                        Long.toString(id)));
        assertEquals(
                "127.0.0.1",
                jdbc.queryForObject(
                        "SELECT ip_address FROM sys_operation_log WHERE target_id = ?",
                        String.class,
                        Long.toString(id)));
    }

    @Test
    void failedAuditInsertRollsBackBusinessWrite() {
        jdbc.execute("DROP TABLE sys_operation_log");
        assertThrows(Exception.class, () -> roles.create(1, "rollback", "Rollback", null, "ENABLED"));
        assertEquals(
                0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE role_code = 'rollback'", Integer.class));
    }

    @Test
    void protectedRoleCannotChangeAndAssignedRoleCannotBeDeleted() {
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> roles.update(1, 1, "Changed", null, "DISABLED"))
                        .code());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> roles.delete(1, 1)).code());
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> roles.delete(1, 10)).code());
        roles.update(1, 10, "Editors", null, "DISABLED");
        assertEquals(0, jdbc.queryForObject("SELECT status FROM sys_role WHERE id = 10", Integer.class));
        jdbc.update("DELETE FROM sys_user_role WHERE role_id = 10");
        roles.delete(1, 10);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE id = 10", Integer.class));
    }

    @Test
    void menuReplacementIsAtomicAndRejectsDisabledMenu() {
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> roles.replaceMenus(1, 10, List.of(101L, 102L)))
                        .code());
        assertEquals(List.of("100"), roles.menus(1, 10));
        roles.replaceMenus(1, 10, List.of(101L));
        assertEquals(List.of("101"), roles.menus(1, 10));
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> roles.replaceMenus(1, 1, List.of()))
                        .code());
    }

    @Test
    void roleAndMenuGrantsSaveTogether() {
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> roles.create(1, "bad_menu", "Bad menu", null, "ENABLED", List.of(100L, 102L)))
                        .code());
        assertEquals(
                0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE role_code = 'bad_menu'", Integer.class));

        long id = roles.create(1, "reader", "Reader", null, "ENABLED", List.of(100L));
        assertEquals(List.of("100"), roles.menus(1, id));
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> roles.update(1, id, "Changed", null, "ENABLED", List.of(102L)))
                        .code());
        assertEquals("Reader", jdbc.queryForObject("SELECT role_name FROM sys_role WHERE id = ?", String.class, id));
        assertEquals(List.of("100"), roles.menus(1, id));

        roles.update(1, id, "Changed", null, "ENABLED", List.of(101L));
        assertEquals("Changed", jdbc.queryForObject("SELECT role_name FROM sys_role WHERE id = ?", String.class, id));
        assertEquals(List.of("101"), roles.menus(1, id));
    }

    @Test
    void customScopeReplacesDeptIdsAndOtherTypesRejectThem() {
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> roles.updateDataScope(1, 10, null, List.of()))
                        .code());
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> roles.updateDataScope(1, 10, "SELF", List.of(20L)))
                        .code());
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> roles.updateDataScope(1, 10, "CUSTOM", List.of(20L, 21L)))
                        .code());
        roles.updateDataScope(1, 10, "CUSTOM", List.of(20L));
        assertEquals("CUSTOM", roles.dataScope(1, 10).type());
        assertEquals(List.of("20"), roles.dataScope(1, 10).deptIds());
        roles.updateDataScope(1, 10, "ALL", List.of());
        assertEquals(List.of(), roles.dataScope(1, 10).deptIds());
    }
}
