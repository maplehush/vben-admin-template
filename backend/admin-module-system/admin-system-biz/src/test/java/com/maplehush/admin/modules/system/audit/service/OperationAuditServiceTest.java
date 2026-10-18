package com.maplehush.admin.modules.system.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.config.RequestMetadataFilter;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class OperationAuditServiceTest {
    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writesTargetAndRequestMetadataWhenAvailable() throws Exception {
        var source =
                new DriverManagerDataSource("jdbc:h2:mem:audit_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE sys_operation_log (actor_user_id BIGINT, action_code VARCHAR(128),
                  target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT,
                  request_id VARCHAR(64), ip_address VARCHAR(45))
                """);
        var service =
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class));
        var request = new MockHttpServletRequest();
        request.setAttribute(RequestMetadataFilter.REQUEST_ID_ATTRIBUTE, "req-123");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(1, "system:role:update", AuditTargetType.ROLE, 42);
        assertEquals("ROLE", jdbc.queryForObject("SELECT target_type FROM sys_operation_log", String.class));
        assertEquals("42", jdbc.queryForObject("SELECT target_id FROM sys_operation_log", String.class));
        assertEquals("req-123", jdbc.queryForObject("SELECT request_id FROM sys_operation_log", String.class));
        assertEquals("127.0.0.1", jdbc.queryForObject("SELECT ip_address FROM sys_operation_log", String.class));

        RequestContextHolder.resetRequestAttributes();
        service.record(2, "user:profile's", AuditTargetType.USER, 43);
        assertEquals(
                "user:profile's",
                jdbc.queryForObject("SELECT action_code FROM sys_operation_log WHERE actor_user_id = 2", String.class));
        assertNull(
                jdbc.queryForObject("SELECT request_id FROM sys_operation_log WHERE actor_user_id = 2", String.class));
        assertNull(
                jdbc.queryForObject("SELECT ip_address FROM sys_operation_log WHERE actor_user_id = 2", String.class));
    }
}
