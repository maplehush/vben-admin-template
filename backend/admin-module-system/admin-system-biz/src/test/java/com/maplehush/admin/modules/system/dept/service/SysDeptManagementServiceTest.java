package com.maplehush.admin.modules.system.dept.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.mapper.SysDeptMapper;
import com.maplehush.admin.modules.system.dept.service.impl.SysDeptServiceImpl;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SysDeptManagementServiceTest {
    private JdbcTemplate jdbc;
    private ISysDeptService departments;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:dept_write_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "CREATE TABLE sys_dept (id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT, dept_code VARCHAR(64) UNIQUE, dept_name VARCHAR(128), sort_order INT, status TINYINT, remark VARCHAR(500))");
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, primary_dept_id BIGINT, status TINYINT)");
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute(
                "CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, auth_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_dept_scope (role_id BIGINT, dept_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'viewer',1,'SELF')");
        jdbc.update("INSERT INTO sys_user VALUES (1,NULL,1), (2,12,1)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update(
                "INSERT INTO sys_dept (id,parent_id,dept_code,dept_name,sort_order,status) VALUES (10,NULL,'root','Root',0,1), (11,10,'child','Child',0,1), (12,11,'leaf','Leaf',0,1), (20,NULL,'other','Other',0,0)");
        departments = new SysDeptServiceImpl(
                TestMapperFactory.create(source).getMapper(SysDeptMapper.class),
                new ManagementPermissionServiceImpl(
                        TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class)),
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class)),
                new DataSourceTransactionManager(source),
                "DEPT_");
    }

    @Test
    void createGeneratesStableCodeAndAllowsDisabledParent() {
        long id = departments.create(1, 10L, "New", 2, "ENABLED", " 研发团队 ");
        assertEquals(10L, jdbc.queryForObject("SELECT parent_id FROM sys_dept WHERE id = ?", Long.class, id));
        assertEquals("研发团队", jdbc.queryForObject("SELECT remark FROM sys_dept WHERE id = ?", String.class, id));
        assertEquals(
                "DEPT_" + String.format("%04d", id),
                jdbc.queryForObject("SELECT dept_code FROM sys_dept WHERE id = ?", String.class, id));
        long childOfDisabled = departments.create(1, 20L, "Child of disabled", 0, "ENABLED", null);
        assertEquals(
                20L, jdbc.queryForObject("SELECT parent_id FROM sys_dept WHERE id = ?", Long.class, childOfDisabled));
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> departments.create(2, null, "No", 0, "ENABLED", null))
                        .code());
        long second = departments.create(1, null, "Generated", 0, "ENABLED", null);
        assertEquals(
                "DEPT_" + String.format("%04d", second),
                jdbc.queryForObject("SELECT dept_code FROM sys_dept WHERE id = ?", String.class, second));
    }

    @Test
    void moveRejectsOwnDescendantButAllowsDisabledParent() {
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> departments.update(1, 10, 12L, "Root", 0, "ENABLED", null))
                        .code());
        departments.update(1, 11, 20L, "Child", 0, "ENABLED", null);
        assertEquals(20L, jdbc.queryForObject("SELECT parent_id FROM sys_dept WHERE id = 11", Long.class));
        departments.update(1, 12, 10L, "Leaf Moved", 3, "ENABLED", "新备注");
        assertEquals(10L, jdbc.queryForObject("SELECT parent_id FROM sys_dept WHERE id = 12", Long.class));
        assertEquals("新备注", jdbc.queryForObject("SELECT remark FROM sys_dept WHERE id = 12", String.class));
    }

    @Test
    void disablingAndDeletingReferencedDepartmentAreRejected() {
        departments.update(1, 11, 10L, "Child", 0, "DISABLED", null);
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> departments.update(1, 12, 11L, "Leaf", 0, "DISABLED", null))
                        .code());
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> departments.delete(1, 11))
                        .code());
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> departments.delete(1, 12))
                        .code());
        jdbc.update("UPDATE sys_user SET primary_dept_id = NULL WHERE id = 2");
        jdbc.update("INSERT INTO sys_role_dept_scope VALUES (2,12)");
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> departments.delete(1, 12))
                        .code());
        jdbc.update("DELETE FROM sys_role_dept_scope WHERE dept_id = 12");
        departments.update(1, 12, 11L, "Leaf", 0, "DISABLED", null);
        departments.delete(1, 12);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_dept WHERE id = 12", Integer.class));
    }
}
