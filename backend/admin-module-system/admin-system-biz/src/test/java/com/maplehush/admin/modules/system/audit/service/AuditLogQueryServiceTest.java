package com.maplehush.admin.modules.system.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.audit.dto.LoginLogQueryDTO;
import com.maplehush.admin.modules.system.audit.dto.OperationLogQueryDTO;
import com.maplehush.admin.modules.system.audit.mapper.AuditLogMapper;
import com.maplehush.admin.modules.system.audit.service.impl.AuditLogQueryServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuditLogQueryServiceTest {
    private JdbcTemplate jdbc;
    private IAuditLogQueryService logs;

    @BeforeEach
    void setUp() throws Exception {
        var source =
                new DriverManagerDataSource("jdbc:h2:mem:audit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(64), primary_dept_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, auth_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_dept_scope (role_id BIGINT, dept_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, parent_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE sys_login_log (id BIGINT PRIMARY KEY, user_id BIGINT,
                  username VARCHAR(64), success TINYINT, failure_reason VARCHAR(255),
                  ip_address VARCHAR(45), user_agent VARCHAR(500), occur_time TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE sys_operation_log (id BIGINT PRIMARY KEY, actor_user_id BIGINT,
                  action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128),
                  success TINYINT, ip_address VARCHAR(45), occur_time TIMESTAMP)
                """);
        jdbc.update("INSERT INTO sys_user VALUES (1,'admin',NULL), (2,'staff',10), (3,'peer',10), (4,'outside',20)");
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'viewer',1,'DEPT')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update(
                "INSERT INTO sys_permission VALUES (1,'system:login-log:list',1), (2,'system:operation-log:list',1)");
        jdbc.update("INSERT INTO sys_role_permission VALUES (2,1), (2,2)");
        jdbc.update(
                "INSERT INTO sys_login_log VALUES (1,2,'staff',1,NULL,'127.0.0.1','Browser','2026-10-02 01:00:00'), (2,3,'peer',0,'bad password',NULL,NULL,'2026-10-02 02:00:00'), (3,4,'outside',1,NULL,NULL,NULL,'2026-10-02 03:00:00'), (4,NULL,'unknown',0,'bad password',NULL,NULL,'2026-10-02 04:00:00')");
        jdbc.update(
                "INSERT INTO sys_operation_log VALUES (1,2,'system:user:update','user','2',1,NULL,'2026-10-02 01:00:00'), (2,4,'system:user:create','user','4',1,NULL,'2026-10-02 02:00:00')");
        logs = new AuditLogQueryServiceImpl(
                TestMapperFactory.create(source).getMapper(AuditLogMapper.class),
                new ManagementPermissionServiceImpl(
                        TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class)));
    }

    @Test
    void loginLogsRespectGrantScopeAndFilters() {
        assertEquals(
                4,
                logs.loginLogs(1, new LoginLogQueryDTO(1, 20, null, null, null, null))
                        .total());
        assertEquals(
                LocalDateTime.of(2026, 10, 2, 4, 0),
                logs.loginLogs(1, new LoginLogQueryDTO(1, 20, null, null, null, null))
                        .items()
                        .getFirst()
                        .occurTime());
        assertEquals(
                2,
                logs.loginLogs(2, new LoginLogQueryDTO(1, 20, null, null, null, null))
                        .total());
        assertEquals(
                "peer",
                logs.loginLogs(2, new LoginLogQueryDTO(1, 20, null, false, null, null))
                        .items()
                        .getFirst()
                        .username());
        assertEquals(
                1,
                logs.loginLogs(
                                2,
                                new LoginLogQueryDTO(
                                        1, 20, "staff", true, "2026-10-01T16:00:00Z", "2026-10-01T17:00:00Z"))
                        .total());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> logs.loginLogs(4, new LoginLogQueryDTO(1, 20, null, null, null, null)))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> logs.loginLogs(2, new LoginLogQueryDTO(0, 20, null, null, null, null)))
                        .code());
    }

    @Test
    void operationLogsRespectGrantScopeAndAction() {
        assertEquals(
                "2",
                logs.operationLogs(1, new OperationLogQueryDTO(1, 20, null, null, null, null))
                        .items()
                        .getFirst()
                        .id());
        assertEquals(
                2,
                logs.operationLogs(1, new OperationLogQueryDTO(1, 20, null, null, null, null))
                        .total());
        assertEquals(
                LocalDateTime.of(2026, 10, 2, 2, 0),
                logs.operationLogs(1, new OperationLogQueryDTO(1, 20, null, null, null, null))
                        .items()
                        .getFirst()
                        .occurTime());
        assertEquals(
                1,
                logs.operationLogs(2, new OperationLogQueryDTO(1, 20, null, null, null, null))
                        .total());
        assertEquals(
                "staff",
                logs.operationLogs(2, new OperationLogQueryDTO(1, 20, "staff", "system:user:update", null, null))
                        .items()
                        .getFirst()
                        .username());
        jdbc.update("UPDATE sys_permission SET status = 0 WHERE id = 2");
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> logs.operationLogs(2, new OperationLogQueryDTO(1, 20, null, null, null, null)))
                        .code());
    }
}
