package com.maplehush.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.modules.system.auth.service.IAccessTokenService;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService;
import com.maplehush.admin.modules.system.auth.service.IOwnPasswordService;
import com.maplehush.admin.modules.system.auth.service.ISessionService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Runs against a disposable MySQL schema; enable explicitly with RUN_MYSQL_E2E=true. */
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_E2E", matches = "true")
class PermissionBoundaryMySqlIT {
    private static final String CREATE_BODY = """
            {"username":"e2e_created","realName":"E2E Created",
             "primaryDeptId":"%d","status":"ENABLED","roleIds":[]}
            """;

    @Test
    void pageButtonAndApiGrantsControlRealHttpRequestsIndependently() throws Exception {
        String password = System.getenv("TEST_DB_PASSWORD");
        if (password == null) throw new IllegalStateException("Set TEST_DB_PASSWORD for the disposable MySQL test");
        String username = System.getenv().getOrDefault("TEST_DB_USERNAME", "root");
        String host = System.getenv().getOrDefault("TEST_MYSQL_HOST", "127.0.0.1");
        String port = System.getenv().getOrDefault("TEST_MYSQL_PORT", "3306");
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/";
        String schema = "maple_hush_it_" + UUID.randomUUID().toString().replace("-", "");
        createSchema(serverUrl, username, password, schema);
        try {
            Flyway.configure()
                    .dataSource(serverUrl + schema, username, password)
                    .locations("classpath:db/migration")
                    .target("9")
                    .load()
                    .migrate();
            try (var connection = DriverManager.getConnection(serverUrl + schema, username, password)) {
                JdbcTemplate beforeGrouping = new JdbcTemplate(
                        new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
                beforeGrouping.update("""
                    INSERT INTO sys_role (role_code, role_name, data_scope_type)
                    VALUES ('e2e_legacy_log', 'Legacy log', 'SELF')
                    """);
                beforeGrouping.update("""
                    INSERT INTO sys_role_menu (role_id, menu_id)
                    SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
                    WHERE r.role_code = 'e2e_legacy_log' AND m.menu_key = 'menu.system.login-log'
                    """);
            }
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                            AdminSystemStartApplication.class)
                    .run(
                            "--spring.profiles.active=local",
                            "--server.port=0",
                            "--spring.datasource.url=" + serverUrl + schema,
                            "--spring.datasource.username=" + username,
                            "--spring.datasource.password=" + password)) {
                JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                assertEquals(
                        schema,
                        jdbc.queryForObject("SELECT DATABASE()", String.class),
                        "The integration test must never write to the development database");
                assertEquals(
                        2, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_dict_item i JOIN sys_dict d ON d.id = i.dict_id
                    WHERE d.dict_code = 'common_status' AND d.built_in = 1
                    """, Integer.class), "The built-in status dictionary must be migrated");
                assertEquals("YES", jdbc.queryForObject("""
                    SELECT IS_NULLABLE FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_dict_item' AND COLUMN_NAME = 'item_color'
                    """, String.class));
                assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_dict_item i JOIN sys_dict d ON d.id = i.dict_id
                    WHERE d.dict_code = 'common_status' AND i.item_value = 'ENABLED' AND i.item_color = 'success'
                    """, Integer.class));
                assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_menu WHERE menu_key = 'menu.system.dict'
                    """, Integer.class), "Dictionary management must have a menu");
                assertEquals(
                        "lucide:book-open-text",
                        jdbc.queryForObject(
                                "SELECT icon FROM sys_menu WHERE menu_key = 'menu.system.dict'", String.class));
                assertEquals(
                        0,
                        jdbc.queryForObject("""
                    SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'sys_%'
                      AND DATA_TYPE = 'datetime' AND DATETIME_PRECISION <> 0
                    """, Integer.class),
                        "Business DATETIME columns must store whole seconds");
                assertEquals(
                        8 * 60 * 60,
                        jdbc.queryForObject(
                                "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(), CURRENT_TIMESTAMP())", Integer.class),
                        "The application's MySQL connections must use Beijing time");
                assertEquals(
                        "mdi:user",
                        jdbc.queryForObject(
                                "SELECT icon FROM sys_menu WHERE menu_key = 'menu.system.user'", String.class));
                assertEquals(
                        "mdi:account-group",
                        jdbc.queryForObject(
                                "SELECT icon FROM sys_menu WHERE menu_key = 'menu.system.role'", String.class));
                assertEquals(
                        "charm:organisation",
                        jdbc.queryForObject(
                                "SELECT icon FROM sys_menu WHERE menu_key = 'menu.system.dept'", String.class));
                assertEquals(
                        "mdi:menu",
                        jdbc.queryForObject(
                                "SELECT icon FROM sys_menu WHERE menu_key = 'menu.system.menu'", String.class));
                assertEquals(
                        1,
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM sys_menu WHERE menu_key = 'menu.system.login-log'",
                                Integer.class));
                assertEquals("menu.system.log", jdbc.queryForObject("""
                    SELECT parent.menu_key FROM sys_menu page
                    JOIN sys_menu parent ON parent.id = page.parent_id
                    WHERE page.menu_key = 'menu.system.login-log'
                    """, String.class));
                assertEquals(
                        "/system/log/login/list",
                        jdbc.queryForObject(
                                "SELECT component FROM sys_menu WHERE menu_key = 'menu.system.login-log'",
                                String.class));
                assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_role_menu rm
                    JOIN sys_role r ON r.id = rm.role_id
                    JOIN sys_menu m ON m.id = rm.menu_id
                    WHERE r.role_code = 'e2e_legacy_log' AND m.menu_key = 'menu.system.log'
                    """, Integer.class));
                assertEquals(
                        1,
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM sys_permission WHERE permission_code = 'system:operation-log:list'",
                                Integer.class));
                long deptId = insertDept(jdbc);
                long roleId = insertRole(jdbc);
                long userId = insertUser(jdbc, roleId, deptId);
                long sessionId = insertSession(jdbc, userId);
                String createBody = CREATE_BODY.formatted(deptId);
                String token = context.getBean(IAccessTokenService.class).issue(userId, sessionId);
                int httpPort = ((ServletWebServerApplicationContext) context)
                        .getWebServer()
                        .getPort();
                String baseUrl = "http://127.0.0.1:" + httpPort;
                HttpClient client = HttpClient.newHttpClient();
                assertEquals(
                        200,
                        request(client, baseUrl, token, "GET", "/system/dict-options/common_status", null)
                                .statusCode());
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/dicts", null)
                                .statusCode());
                grantApi(jdbc, roleId, "system:dict:list");
                assertEquals(
                        200,
                        request(client, baseUrl, token, "GET", "/system/dicts", null)
                                .statusCode());

                // A page grant exposes navigation but cannot authorize a button or an API.
                grantMenu(jdbc, roleId, "menu.system");
                grantMenu(jdbc, roleId, "menu.system.user");
                assertTrue(request(client, baseUrl, token, "GET", "/menu/all", null)
                        .body()
                        .contains("\"path\":\"/system/user\""));
                assertFalse(request(client, baseUrl, token, "GET", "/auth/codes", null)
                        .body()
                        .contains("system:user:create"));
                assertEquals(
                        403,
                        request(client, baseUrl, token, "POST", "/system/users", createBody)
                                .statusCode());
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/users", null)
                                .statusCode());
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/login-logs", null)
                                .statusCode());

                // A button grant allows its operation but does not imply the separate list API.
                grantButton(jdbc, roleId, "system:user:create");
                assertTrue(request(client, baseUrl, token, "GET", "/auth/codes", null)
                        .body()
                        .contains("\"system:user:create\""));
                assertEquals(
                        200,
                        request(client, baseUrl, token, "POST", "/system/users", createBody)
                                .statusCode());
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/users", null)
                                .statusCode());

                // An independent API grant takes effect immediately and can be revoked independently.
                grantApi(jdbc, roleId, "system:user:list");
                assertTrue(request(client, baseUrl, token, "GET", "/auth/codes", null)
                        .body()
                        .contains("\"system:user:list\""));
                assertEquals(
                        200,
                        request(client, baseUrl, token, "GET", "/system/users", null)
                                .statusCode());
                grantMenu(jdbc, roleId, "menu.system.login-log");
                grantMenu(jdbc, roleId, "menu.system.operation-log");
                assertTrue(request(client, baseUrl, token, "GET", "/menu/all", null)
                        .body()
                        .contains("\"path\":\"/system/log/login\""));
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/operation-logs", null)
                                .statusCode());
                grantApi(jdbc, roleId, "system:login-log:list");
                grantApi(jdbc, roleId, "system:operation-log:list");
                assertEquals(
                        200,
                        request(client, baseUrl, token, "GET", "/system/login-logs", null)
                                .statusCode());
                assertEquals(
                        200,
                        request(client, baseUrl, token, "GET", "/system/operation-logs", null)
                                .statusCode());
                jdbc.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/users", null)
                                .statusCode());
                assertEquals(
                        403,
                        request(client, baseUrl, token, "GET", "/system/login-logs", null)
                                .statusCode());
                assertFalse(request(client, baseUrl, token, "GET", "/auth/codes", null)
                        .body()
                        .contains("system:user:list"));
                jdbc.update(
                        "DELETE FROM sys_role_menu WHERE role_id = ? AND menu_id = "
                                + "(SELECT id FROM sys_menu WHERE auth_code = 'system:user:create')",
                        roleId);
                assertEquals(
                        403,
                        request(client, baseUrl, token, "POST", "/system/users", createBody)
                                .statusCode());
                assertFalse(request(client, baseUrl, token, "GET", "/auth/codes", null)
                        .body()
                        .contains("system:user:create"));

                verifyManagementFlows(jdbc, context, client, baseUrl);
            }
        } finally {
            dropSchema(serverUrl, username, password, schema);
        }
    }

    private void verifyManagementFlows(
            JdbcTemplate jdbc, ConfigurableApplicationContext context, HttpClient client, String baseUrl)
            throws Exception {
        jdbc.update("INSERT INTO sys_user (username, password_hash, real_name) "
                + "VALUES ('e2e_admin', 'unused', 'E2E Admin')");
        long adminId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username = 'e2e_admin'", Long.class);
        long superRoleId = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = 'super_admin'", Long.class);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", adminId, superRoleId);
        long adminSessionId = insertSession(jdbc, adminId);
        String adminToken = context.getBean(IAccessTokenService.class).issue(adminId, adminSessionId);

        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/dicts", """
                {"dictCode":"e2e_audit","dictName":"E2E Audit","status":"ENABLED"}
                """)
                        .statusCode());
        long dictId = jdbc.queryForObject("SELECT id FROM sys_dict WHERE dict_code = 'e2e_audit'", Long.class);
        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/dicts/" + dictId + "/items", """
                {"itemValue":"1","itemLabel":"Enabled","sortOrder":1,"status":"ENABLED"}
                """)
                        .statusCode());
        long itemId = jdbc.queryForObject(
                "SELECT id FROM sys_dict_item WHERE dict_id = ? AND item_value = '1'", Long.class, dictId);
        assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_operation_log
                    WHERE actor_user_id = ? AND action_code = 'system:dict:create'
                      AND target_type = 'DICT' AND target_id = ? AND success = 1
                    """, Integer.class, adminId, Long.toString(dictId)));
        assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM sys_operation_log
                    WHERE actor_user_id = ? AND action_code = 'system:dict-item:create'
                      AND target_type = 'DICT_ITEM' AND target_id = ? AND success = 1
                    """, Integer.class, adminId, Long.toString(itemId)));

        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/depts", """
                {"deptName":"E2E Parent","sortOrder":0,"status":"ENABLED"}
                """)
                        .statusCode());
        long parentId = jdbc.queryForObject("SELECT id FROM sys_dept WHERE dept_name = 'E2E Parent'", Long.class);
        assertEquals(
                "DEPT_%04d".formatted(parentId),
                jdbc.queryForObject("SELECT dept_code FROM sys_dept WHERE id = ?", String.class, parentId));
        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/depts", """
                {"parentId":"%d","deptName":"E2E Child","sortOrder":0,"status":"ENABLED"}
                """.formatted(parentId))
                        .statusCode());
        long childId = jdbc.queryForObject("SELECT id FROM sys_dept WHERE dept_name = 'E2E Child'", Long.class);
        assertEquals(
                409,
                request(client, baseUrl, adminToken, "DELETE", "/system/depts/" + parentId, null)
                        .statusCode());
        assertEquals(
                200,
                request(client, baseUrl, adminToken, "GET", "/system/depts/tree", null)
                        .statusCode());

        long menuId = jdbc.queryForObject("SELECT id FROM sys_menu WHERE menu_key = 'menu.system.user'", Long.class);
        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/roles", """
                {"roleCode":"e2e_managed","roleName":"E2E Managed","status":"ENABLED",
                 "menuIds":["%d"]}
                """.formatted(menuId))
                        .statusCode());
        long managedRoleId = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = 'e2e_managed'", Long.class);
        assertTrue(request(client, baseUrl, adminToken, "GET", "/system/roles/" + managedRoleId + "/menus", null)
                .body()
                .contains("\"" + menuId + "\""));

        assertEquals(
                200,
                request(client, baseUrl, adminToken, "POST", "/system/users", """
                {"username":"e2e_managed_user","realName":"E2E Managed User",
                 "primaryDeptId":"%d","status":"ENABLED","roleIds":["%d"]}
                """.formatted(childId, managedRoleId))
                        .statusCode());
        long managedUserId =
                jdbc.queryForObject("SELECT id FROM sys_user WHERE username = 'e2e_managed_user'", Long.class);
        HttpResponse<String> detail =
                request(client, baseUrl, adminToken, "GET", "/system/users/" + managedUserId, null);
        assertEquals(200, detail.statusCode());
        LocalDateTime storedBeijingTime = jdbc.queryForObject(
                "SELECT create_time FROM sys_user WHERE id = ?", LocalDateTime.class, managedUserId);
        long offsetFromUtc = jdbc.queryForObject(
                "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(3), create_time) " + "FROM sys_user WHERE id = ?",
                Long.class,
                managedUserId);
        assertTrue(
                offsetFromUtc >= 8 * 60 * 60 - 10 && offsetFromUtc <= 8 * 60 * 60 + 10,
                "A newly created user must be stored using Beijing wall-clock time");
        String displayedTime = storedBeijingTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        assertTrue(detail.body().contains("\"createTime\":\"" + displayedTime + "\""));
        assertEquals(
                200,
                request(
                                client,
                                baseUrl,
                                adminToken,
                                "PUT",
                                "/system/users/" + managedUserId + "/status",
                                "{\"status\":\"DISABLED\"}")
                        .statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT status FROM sys_user WHERE id = ?", Integer.class, managedUserId));
        assertEquals(
                200,
                request(
                                client,
                                baseUrl,
                                adminToken,
                                "PUT",
                                "/system/users/" + managedUserId + "/roles",
                                "{\"roleIds\":[]}")
                        .statusCode());
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?", Integer.class, managedUserId));

        verifyAuthenticationPersistence(jdbc, context, adminId);
    }

    private void verifyAuthenticationPersistence(
            JdbcTemplate jdbc, ConfigurableApplicationContext context, long adminId) {
        ISessionService sessions = context.getBean(ISessionService.class);
        ICurrentUserService currentUsers = context.getBean(ICurrentUserService.class);
        var pair = sessions.create(adminId, "127.0.0.1", "integration-test");
        assertEquals(adminId, currentUsers.authenticate(pair.accessToken()).userId());
        var refreshed = sessions.refresh(pair.refreshToken());
        assertThrows(ApiException.class, () -> sessions.refresh(pair.refreshToken()));
        assertEquals(adminId, currentUsers.authenticate(refreshed.accessToken()).userId());
        sessions.revoke(refreshed.refreshToken());
        assertThrows(ApiException.class, () -> currentUsers.authenticate(refreshed.accessToken()));

        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
        jdbc.update("UPDATE sys_user SET password_hash = ? WHERE id = ?", encoder.encode("old-password-123"), adminId);
        context.getBean(IOwnPasswordService.class).change(adminId, "old-password-123", "new-password-456");
        assertEquals(
                0,
                jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = ?", Integer.class, adminId));
    }

    private long insertDept(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO sys_dept (dept_code, dept_name) VALUES ('e2e_dept', 'E2E Department')");
        return jdbc.queryForObject("SELECT id FROM sys_dept WHERE dept_code = 'e2e_dept'", Long.class);
    }

    private long insertRole(JdbcTemplate jdbc) {
        jdbc.update(
                "INSERT INTO sys_role (role_code, role_name, data_scope_type) VALUES ('e2e_reader', 'E2E Reader', 'DEPT')");
        return jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = 'e2e_reader'", Long.class);
    }

    private long insertUser(JdbcTemplate jdbc, long roleId, long deptId) {
        jdbc.update(
                "INSERT INTO sys_user (username, password_hash, real_name, primary_dept_id) "
                        + "VALUES ('e2e_reader', 'unused', 'E2E Reader', ?)",
                deptId);
        long userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username = 'e2e_reader'", Long.class);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        return userId;
    }

    private long insertSession(JdbcTemplate jdbc, long userId) {
        String refreshHash = UUID.randomUUID().toString().replace("-", "").repeat(2);
        jdbc.update(
                "INSERT INTO sys_session (user_id, refresh_token_hash, expire_time) "
                        + "VALUES (?, ?, DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 1 HOUR))",
                userId,
                refreshHash);
        return jdbc.queryForObject("SELECT id FROM sys_session WHERE refresh_token_hash = ?", Long.class, refreshHash);
    }

    private void grantMenu(JdbcTemplate jdbc, long roleId, String key) {
        jdbc.update(
                "INSERT INTO sys_role_menu (role_id, menu_id) " + "SELECT ?, id FROM sys_menu WHERE menu_key = ?",
                roleId,
                key);
    }

    private void grantButton(JdbcTemplate jdbc, long roleId, String code) {
        jdbc.update(
                "INSERT INTO sys_role_menu (role_id, menu_id) " + "SELECT ?, id FROM sys_menu WHERE auth_code = ?",
                roleId,
                code);
    }

    private void grantApi(JdbcTemplate jdbc, long roleId, String code) {
        jdbc.update(
                "INSERT INTO sys_role_permission (role_id, permission_id) "
                        + "SELECT ?, id FROM sys_permission WHERE permission_code = ?",
                roleId,
                code);
    }

    private HttpResponse<String> request(
            HttpClient client, String baseUrl, String token, String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token);
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void createSchema(String serverUrl, String username, String password, String schema) throws Exception {
        try (var connection = DriverManager.getConnection(serverUrl, username, password);
                var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
    }

    private void dropSchema(String serverUrl, String username, String password, String schema) throws Exception {
        try (var connection = DriverManager.getConnection(serverUrl, username, password);
                var statement = connection.createStatement()) {
            statement.execute("DROP DATABASE `" + schema + "`");
        }
    }
}
