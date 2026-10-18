package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.config.PasswordConfiguration;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.impl.OwnPasswordServiceImpl;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

class OwnPasswordServiceTest {

    private JdbcTemplate jdbc;
    private PasswordEncoder encoder;
    private IOwnPasswordService service;
    private DriverManagerDataSource source;
    private AuthUserMapper users;
    private SysSessionMapper sessions;

    @BeforeEach
    void setUp() throws Exception {
        source = new DriverManagerDataSource(
                "jdbc:h2:mem:password_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT PRIMARY KEY, password_hash VARCHAR(255),
                  must_change_password TINYINT, password_change_time TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE sys_session (id BIGINT PRIMARY KEY, user_id BIGINT, revoke_time TIMESTAMP)");
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        encoder = new PasswordConfiguration().passwordEncoder();
        jdbc.update("INSERT INTO sys_user VALUES (1, ?, 1, NULL)", encoder.encode("old-password-123"));
        jdbc.update("INSERT INTO sys_session VALUES (9, 1, NULL)");
        var mappers = TestMapperFactory.create(source);
        users = mappers.getMapper(AuthUserMapper.class);
        sessions = mappers.getMapper(SysSessionMapper.class);
        service = serviceWithAudit(new OperationAuditServiceImpl(mappers.getMapper(OperationAuditMapper.class)));
    }

    @Test
    void changeClearsFlagAndRevokesEverySession() {
        service.change(1, "old-password-123", "new-password-456");
        assertTrue(encoder.matches(
                "new-password-456",
                jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = 1", String.class)));
        assertEquals(0, jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = 1", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE user_id = 1 AND revoke_time IS NOT NULL",
                        Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_operation_log WHERE actor_user_id = 1 AND action_code = 'user:password' AND target_type = 'USER' AND target_id = '1'",
                        Integer.class));
    }

    @Test
    void acceptsEightCharacterNewPasswordButRejectsSeven() {
        assertThrows(ApiException.class, () -> service.change(1, "old-password-123", "Seven12"));
        service.change(1, "old-password-123", "Eight123");
        assertTrue(encoder.matches(
                "Eight123", jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = 1", String.class)));
    }

    @Test
    void wrongOldPasswordLeavesSessionAndPasswordUntouched() {
        assertThrows(ApiException.class, () -> service.change(1, "wrong-password", "new-password-456"));
        assertEquals(1, jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = 1", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE user_id = 1 AND revoke_time IS NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log", Integer.class));
    }

    @Test
    void auditFailureRollsBackPasswordChangeAndSessionRevocation() {
        IOperationAuditService failingAudit = (actor, action, type, target) -> {
            throw new IllegalStateException("audit unavailable");
        };
        assertThrows(
                IllegalStateException.class,
                () -> serviceWithAudit(failingAudit).change(1, "old-password-123", "new-password-456"));
        assertEquals(1, jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = 1", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_session WHERE user_id = 1 AND revoke_time IS NULL", Integer.class));
    }

    private IOwnPasswordService serviceWithAudit(IOperationAuditService audit) {
        return new OwnPasswordServiceImpl(
                users,
                sessions,
                encoder,
                new DataSourceTransactionManager(source),
                audit,
                Clock.system(DateUtils.ZONE));
    }
}
