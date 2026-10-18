package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.auth.config.AuthProperties;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.service.impl.AccessTokenServiceImpl;
import com.maplehush.admin.modules.system.auth.service.impl.CurrentUserServiceImpl;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class CurrentUserServiceTest {

    private JdbcTemplate jdbc;
    private IAccessTokenService tokens;
    private ICurrentUserService currentUsers;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:current_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, status TINYINT, must_change_password TINYINT)");
        jdbc.execute(
                "CREATE TABLE sys_session (id BIGINT PRIMARY KEY, user_id BIGINT, expire_time TIMESTAMP, revoke_time TIMESTAMP)");
        jdbc.update("INSERT INTO sys_user VALUES (1, 1, 1)");
        jdbc.update("INSERT INTO sys_session VALUES (9, 1, DATEADD('DAY', 1, CURRENT_TIMESTAMP), NULL)");
        var props = new AuthProperties(
                "test-issuer",
                "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "/api/auth",
                false,
                List.of("http://localhost"));
        tokens = new AccessTokenServiceImpl(props, Clock.system(DateUtils.ZONE));
        currentUsers = new CurrentUserServiceImpl(
                TestMapperFactory.create(source).getMapper(AuthUserMapper.class), tokens, Clock.system(DateUtils.ZONE));
    }

    @Test
    void tokenRequiresLiveSessionAndReflectsCurrentPasswordFlag() {
        String token = tokens.issue(1, 9);
        assertTrue(currentUsers.authenticate(token).mustChangePassword());
        jdbc.update("UPDATE sys_user SET must_change_password = 0 WHERE id = 1");
        assertEquals(1, currentUsers.authenticate(token).userId());
        jdbc.update("UPDATE sys_session SET revoke_time = CURRENT_TIMESTAMP WHERE id = 9");
        assertThrows(ApiException.class, () -> currentUsers.authenticate(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokens.issue(1, 9);
        assertThrows(ApiException.class, () -> currentUsers.authenticate(token + "x"));
    }
}
