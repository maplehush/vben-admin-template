package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.dto.SessionQueryDTO;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.impl.SessionManagementServiceImpl;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class SessionManagementServiceTest {
    private JdbcTemplate jdbc;
    private ISessionManagementService sessions;
    private DriverManagerDataSource source;
    private SysSessionMapper mapper;
    private IOperationAuditService audit;
    private ManagementPermissionServiceImpl permissions;

    @BeforeEach
    void setUp() throws Exception {
        source = new DriverManagerDataSource(
                "jdbc:h2:mem:session_management_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, auth_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(64))");
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        jdbc.execute("""
                CREATE TABLE sys_session (id BIGINT PRIMARY KEY, user_id BIGINT, device_info VARCHAR(500),
                  ip_address VARCHAR(45), expire_time TIMESTAMP, revoke_time TIMESTAMP,
                  create_time TIMESTAMP)
                """);
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'operator',1,'ALL')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update("INSERT INTO sys_permission VALUES (1,'system:session:list',1), (2,'system:session:revoke',1)");
        jdbc.update("INSERT INTO sys_role_permission VALUES (2,1), (2,2)");
        jdbc.update("INSERT INTO sys_user VALUES (1,'admin'), (2,'operator'), (3,'other')");
        var future = LocalDateTime.now(DateUtils.ZONE).plusDays(1);
        var past = LocalDateTime.now(DateUtils.ZONE).minusDays(1);
        jdbc.update("INSERT INTO sys_session VALUES (10,1,'Browser','127.0.0.1',?,NULL,?)", future, past);
        jdbc.update("INSERT INTO sys_session VALUES (11,2,'Phone','127.0.0.2',?,NULL,?)", future, past);
        jdbc.update("INSERT INTO sys_session VALUES (12,3,'Old','127.0.0.3',?,NULL,?)", past, past);
        jdbc.update("INSERT INTO sys_session VALUES (13,3,'Browser','127.0.0.3',?,NULL,?)", future, past);
        var mappers = TestMapperFactory.create(source);
        mapper = mappers.getMapper(SysSessionMapper.class);
        audit = new OperationAuditServiceImpl(mappers.getMapper(OperationAuditMapper.class));
        permissions = new ManagementPermissionServiceImpl(mappers.getMapper(ManagementPermissionMapper.class));
        sessions = transactionalService(audit);
    }

    @Test
    void ownListAndRevokeCannotAffectAnotherUser() {
        assertEquals(1, sessions.own(1).size());
        assertEquals("10", sessions.own(1).getFirst().id());
        assertEquals(true, sessions.own(1, 10).getFirst().current());
        assertEquals(
                false,
                sessions.all(2, new SessionQueryDTO(1, 20, "admin"))
                        .items()
                        .getFirst()
                        .current());
        assertEquals(
                40400,
                assertThrows(ApiException.class, () -> sessions.revokeOwn(1, 11))
                        .code());
        sessions.revokeOwn(1, 10);
        assertEquals(0, sessions.own(1).size());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_operation_log WHERE actor_user_id = 1 AND action_code = 'auth:session:revoke' AND target_type = 'SESSION' AND target_id = '10'",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE id = 10 AND revoke_time IS NOT NULL", Integer.class));
    }

    @Test
    void managerRequiresPermissionAndOnlySeesActiveSessions() {
        assertEquals(3, sessions.all(2, new SessionQueryDTO(1, 20, null)).total());
        assertEquals(1, sessions.all(2, new SessionQueryDTO(1, 20, "admin")).total());
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> sessions.all(2, new SessionQueryDTO(0, 20, null)))
                        .code());
        jdbc.update("UPDATE sys_permission SET status = 0 WHERE id = 1");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> sessions.all(2, new SessionQueryDTO(1, 20, null)))
                        .code());
    }

    @Test
    void managerRevokeImmediatelyMarksTargetAndRequiresGrant() {
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> sessions.revokeManaged(2, 10))
                        .code());
        sessions.revokeManaged(2, 13);
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_operation_log WHERE actor_user_id = 2 AND action_code = 'system:session:revoke' AND target_type = 'SESSION' AND target_id = '13'",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE id = 13 AND revoke_time IS NOT NULL", Integer.class));
        assertEquals(
                40400,
                assertThrows(ApiException.class, () -> sessions.revokeManaged(2, 99))
                        .code());
        jdbc.update("UPDATE sys_permission SET status = 0 WHERE id = 2");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> sessions.revokeManaged(2, 11))
                        .code());
    }

    @Test
    void auditFailureRollsBackManagedSessionRevoke() {
        IOperationAuditService failingAudit = (actor, action, type, target) -> {
            throw new IllegalStateException("audit unavailable");
        };
        assertThrows(
                IllegalStateException.class,
                () -> transactionalService(failingAudit).revokeManaged(2, 13));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE id = 13 AND revoke_time IS NULL", Integer.class));
    }

    private ISessionManagementService transactionalService(IOperationAuditService operationAudit) {
        var target =
                new SessionManagementServiceImpl(mapper, permissions, operationAudit, Clock.system(DateUtils.ZONE));
        var factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        return (ISessionManagementService) factory.getProxy();
    }
}
