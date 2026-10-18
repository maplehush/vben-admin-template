package com.maplehush.admin.modules.system.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.config.PasswordConfiguration;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.mapper.UserAccessMapper;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.auth.service.impl.UserAccessServiceImpl;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import com.maplehush.admin.modules.system.user.mapper.SysUserMapper;
import com.maplehush.admin.modules.system.user.service.impl.SysUserServiceImpl;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

class SysUserManagementServiceTest {
    private JdbcTemplate jdbc;
    private PasswordEncoder encoder;
    private ISysUserService users;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:user_write_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        encoder = new PasswordConfiguration().passwordEncoder();
        jdbc.execute(
                "CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, parent_id BIGINT, status TINYINT, dept_name VARCHAR(128))");
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) UNIQUE,
                  password_hash VARCHAR(255), real_name VARCHAR(128), email VARCHAR(255) UNIQUE,
                  phone VARCHAR(32), avatar_url VARCHAR(1024), primary_dept_id BIGINT,
                  remark VARCHAR(500),
                  home_path VARCHAR(255), status TINYINT, must_change_password TINYINT,
                  password_change_time TIMESTAMP, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT, PRIMARY KEY(user_id, role_id))");
        jdbc.execute("""
                CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, menu_type VARCHAR(16),
                  route_name VARCHAR(128), route_path VARCHAR(255), component VARCHAR(255),
                  redirect_path VARCHAR(255), auth_code VARCHAR(128), title_key VARCHAR(255),
                  icon VARCHAR(128), sort_order INT, status TINYINT, route_meta VARCHAR(4096))
                """);
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_dept_scope (role_id BIGINT, dept_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_session (id BIGINT PRIMARY KEY, user_id BIGINT, revoke_time TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64),
                  target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))
                """);
        jdbc.update("INSERT INTO sys_dept VALUES (10,NULL,1,'Root'), (11,10,1,'Child'), (20,NULL,1,'Elsewhere')");
        String hash = encoder.encode("ExistingSecret123");
        jdbc.update("""
                INSERT INTO sys_user (id,username,password_hash,real_name,primary_dept_id,status,must_change_password)
                VALUES (1,'root',?,'Root',NULL,1,0), (2,'manager',?,'Manager',10,1,0),
                       (3,'child',?,'Child',11,1,0), (4,'outside',?,'Outside',20,1,0)
                """, hash, hash, hash, hash);
        jdbc.update(
                "INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'manager',1,'DEPT_AND_CHILDREN'), (3,'viewer',1,'SELF'), (4,'disabled',0,'ALL')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2), (3,3)");
        String[] codes = {"system:user:create", "system:user:update", "system:user:status", "system:user:reset-password"
        };
        for (int i = 0; i < codes.length; i++) {
            jdbc.update(
                    "INSERT INTO sys_menu VALUES (?,NULL,'BUTTON',NULL,NULL,NULL,NULL,?,'action',NULL,0,1,NULL)",
                    100 + i,
                    codes[i]);
            jdbc.update("INSERT INTO sys_role_menu VALUES (2,?)", 100 + i);
        }
        var permissions = new ManagementPermissionServiceImpl(
                TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class));
        var mappers = TestMapperFactory.create(source);
        users = new SysUserServiceImpl(
                mappers.getMapper(SysUserMapper.class),
                mappers.getMapper(SysSessionMapper.class),
                permissions,
                encoder,
                new UserAccessServiceImpl(
                        TestMapperFactory.create(source).getMapper(UserAccessMapper.class), new ObjectMapper()),
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class)),
                new DataSourceTransactionManager(source),
                Clock.system(DateUtils.ZONE));
    }

    @Test
    void createHashesPasswordAndRequiresChange() {
        long id = users.create(
                1,
                new ISysUserService.CreateUser(
                        "new.user",
                        new ISysUserService.UserData("New User", null, null, 10L, null, null),
                        "ENABLED",
                        List.of(3L)));
        String hash = jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = ?", String.class, id);
        assertTrue(encoder.matches("12345678", hash));
        assertEquals(
                1, jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = ?", Integer.class, id));
        assertEquals(3, jdbc.queryForObject("SELECT role_id FROM sys_user_role WHERE user_id = ?", Integer.class, id));
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> users.create(
                                        1,
                                        new ISysUserService.CreateUser(
                                                "new.user",
                                                new ISysUserService.UserData("Again", null, null, 10L, null, null),
                                                "ENABLED",
                                                List.of())))
                        .code());
    }

    @Test
    void ordinaryUsersMustBelongToAnActiveDepartment() {
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> users.create(
                                        1,
                                        new ISysUserService.CreateUser(
                                                "no.dept",
                                                new ISysUserService.UserData("No Dept", null, null, null, null, null),
                                                "ENABLED",
                                                List.of())))
                        .code());
        assertEquals(
                40000,
                assertThrows(
                                ApiException.class,
                                () -> users.update(
                                        1, 3, new ISysUserService.UserData("Child", null, null, null, null, null)))
                        .code());
        users.update(1, 1, new ISysUserService.UserData("Root", null, null, null, null, null));
        assertEquals(null, jdbc.queryForObject("SELECT primary_dept_id FROM sys_user WHERE id = 1", Long.class));
    }

    @Test
    void temporaryPasswordsRequireAtLeastEightCharacters() {
        long id = users.create(
                1,
                new ISysUserService.CreateUser(
                        "eight.user",
                        new ISysUserService.UserData("Eight", null, null, 10L, null, null),
                        "ENABLED",
                        List.of()));
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> users.resetPassword(1, id, "Seven12"))
                        .code());
        users.resetPassword(1, id, "Reset123");
        assertTrue(encoder.matches(
                "Reset123", jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = ?", String.class, id)));
    }

    @Test
    void userRemarkPersistsOnCreateAndUpdate() {
        long id = users.create(
                1,
                new ISysUserService.CreateUser(
                        "remark.user",
                        new ISysUserService.UserData("Remark User", null, null, 10L, null, null, "first"),
                        "ENABLED",
                        List.of()));
        assertEquals("first", jdbc.queryForObject("SELECT remark FROM sys_user WHERE id = ?", String.class, id));
        users.update(1, id, new ISysUserService.UserData("Remark User", null, null, 10L, null, null, "revised"));
        assertEquals("revised", jdbc.queryForObject("SELECT remark FROM sys_user WHERE id = ?", String.class, id));
    }

    @Test
    void ordinaryManagerCannotCreateOutsideScopeOrAssignRoles() {
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.create(
                                        2,
                                        new ISysUserService.CreateUser(
                                                "outside2",
                                                new ISysUserService.UserData("Outside", null, null, 20L, null, null),
                                                "ENABLED",
                                                List.of())))
                        .code());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.create(
                                        2,
                                        new ISysUserService.CreateUser(
                                                "inside2",
                                                new ISysUserService.UserData("Inside", null, null, 11L, null, null),
                                                "ENABLED",
                                                List.of(3L))))
                        .code());
        long id = users.create(
                2,
                new ISysUserService.CreateUser(
                        "inside3",
                        new ISysUserService.UserData("Inside", null, null, 11L, null, null),
                        "ENABLED",
                        List.of()));
        assertTrue(id > 4);
    }

    @Test
    void updateChecksCurrentAndDestinationScope() {
        users.update(2, 3, new ISysUserService.UserData("Changed", null, null, 11L, null, null));
        assertEquals("Changed", jdbc.queryForObject("SELECT real_name FROM sys_user WHERE id = 3", String.class));
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.update(
                                        2, 4, new ISysUserService.UserData("No", null, null, 20L, null, null)))
                        .code());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.update(
                                        2, 3, new ISysUserService.UserData("No", null, null, 20L, null, null)))
                        .code());
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.update(
                                        2, 1, new ISysUserService.UserData("No", null, null, null, null, null)))
                        .code());
    }

    @Test
    void disablingUserRevokesSessionsButCannotDisableLastSuperAdmin() {
        jdbc.update("INSERT INTO sys_session VALUES (50,3,NULL)");
        users.changeStatus(2, 3, "DISABLED");
        assertEquals(0, jdbc.queryForObject("SELECT status FROM sys_user WHERE id = 3", Integer.class));
        assertTrue(
                jdbc.queryForObject("SELECT revoke_time FROM sys_session WHERE id = 50", java.time.LocalDateTime.class)
                        != null);
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.changeStatus(1, 1, "DISABLED"))
                        .code());
    }

    @Test
    void userCannotDisableOwnAccount() {
        jdbc.update("INSERT INTO sys_session VALUES (52,2,NULL)");
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.changeStatus(2, 2, "DISABLED"))
                        .code());
        assertEquals(1, jdbc.queryForObject("SELECT status FROM sys_user WHERE id = 2", Integer.class));
        assertEquals(
                null,
                jdbc.queryForObject(
                        "SELECT revoke_time FROM sys_session WHERE id = 52", java.time.LocalDateTime.class));
    }

    @Test
    void userWithoutStatusPermissionCannotDisableAnotherAccount() {
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.changeStatus(3, 4, "DISABLED"))
                        .code());
        assertEquals(1, jdbc.queryForObject("SELECT status FROM sys_user WHERE id = 4", Integer.class));
    }

    @Test
    void roleReplacementValidatesAllRolesAndProtectsSystemAccount() {
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> users.replaceRoles(1, 3, List.of(3L, 4L)))
                        .code());
        assertEquals(3, jdbc.queryForObject("SELECT role_id FROM sys_user_role WHERE user_id = 3", Integer.class));
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.replaceRoles(1, 1, List.of(3L)))
                        .code());
        users.replaceRoles(1, 3, List.of(2L));
        assertEquals(2, jdbc.queryForObject("SELECT role_id FROM sys_user_role WHERE user_id = 3", Integer.class));
    }

    @Test
    void superAdminRoleCannotBeAssignedThroughUserManagement() {
        assertEquals(
                40300,
                assertThrows(
                                ApiException.class,
                                () -> users.create(
                                        1,
                                        new ISysUserService.CreateUser(
                                                "another.admin",
                                                new ISysUserService.UserData("Another", null, null, 10L, null, null),
                                                "ENABLED",
                                                List.of(1L))))
                        .code());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.replaceRoles(1, 3, List.of(1L)))
                        .code());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> users.replaceRoles(1, 1, List.of(1L)))
                        .code());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_role WHERE role_id = 1", Integer.class));
    }

    @Test
    void resetPasswordRevokesSessionsAndProfileOnlyChangesSelf() {
        jdbc.update("INSERT INTO sys_session VALUES (51,3,NULL)");
        users.resetPassword(2, 3, "TemporarySecret123");
        assertTrue(encoder.matches(
                "TemporarySecret123",
                jdbc.queryForObject("SELECT password_hash FROM sys_user WHERE id = 3", String.class)));
        assertEquals(1, jdbc.queryForObject("SELECT must_change_password FROM sys_user WHERE id = 3", Integer.class));
        assertTrue(
                jdbc.queryForObject("SELECT revoke_time FROM sys_session WHERE id = 51", java.time.LocalDateTime.class)
                        != null);
        users.updateProfile(2, new ISysUserService.ProfileData("My New Name", null, null, null));
        assertEquals("My New Name", jdbc.queryForObject("SELECT real_name FROM sys_user WHERE id = 2", String.class));
        assertFalse(
                "My New Name".equals(jdbc.queryForObject("SELECT real_name FROM sys_user WHERE id = 3", String.class)));
    }
}
