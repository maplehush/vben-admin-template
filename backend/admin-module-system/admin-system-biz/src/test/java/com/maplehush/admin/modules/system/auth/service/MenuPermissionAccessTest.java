package com.maplehush.admin.modules.system.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.maplehush.admin.modules.system.auth.mapper.UserAccessMapper;
import com.maplehush.admin.modules.system.auth.service.impl.UserAccessServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

class MenuPermissionAccessTest {
    @Test
    void menuVisibilityAndButtonApiCodesUseSeparateGrants() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:split_access_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(64), real_name VARCHAR(128),
                  avatar_url VARCHAR(255), home_path VARCHAR(255), must_change_password TINYINT)
                """);
        jdbc.execute("""
                CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, menu_type VARCHAR(16),
                  route_name VARCHAR(128), route_path VARCHAR(255), component VARCHAR(255),
                  redirect_path VARCHAR(255), auth_code VARCHAR(128), title_key VARCHAR(255),
                  icon VARCHAR(128), sort_order INT, status TINYINT, route_meta VARCHAR(4096))
                """);
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.update("INSERT INTO sys_user VALUES (1,'reader','Reader',NULL,NULL,0)");
        jdbc.update("INSERT INTO sys_role VALUES (1,'reader',1)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1)");
        jdbc.update("""
                INSERT INTO sys_menu VALUES
                (10,NULL,'CATALOG','System','/system',NULL,NULL,NULL,'system.title',NULL,0,1,NULL),
                (11,10,'MENU','Users','/system/user','/system/user/list',NULL,NULL,'system.user.title',NULL,1,1,NULL),
                (12,11,'BUTTON',NULL,NULL,NULL,NULL,'system:user:create','新增用户',NULL,1,1,NULL)
                """);
        jdbc.update("INSERT INTO sys_permission VALUES (20,'system:user:list',1)");
        jdbc.update("INSERT INTO sys_role_menu VALUES (1,11)");
        var access = new UserAccessServiceImpl(
                TestMapperFactory.create(source).getMapper(UserAccessMapper.class), new ObjectMapper());
        assertEquals("Users", access.menus(1).getFirst().children().getFirst().name());
        assertTrue(access.codes(1).isEmpty());
        jdbc.update("INSERT INTO sys_role_menu VALUES (1,12)");
        assertEquals("system:user:create", access.codes(1).getFirst());
        jdbc.update("INSERT INTO sys_role_permission VALUES (1,20)");
        assertTrue(access.codes(1).contains("system:user:list"));
        jdbc.update("UPDATE sys_menu SET status = 0 WHERE id = 11");
        assertTrue(access.menus(1).isEmpty());
        assertFalse(access.codes(1).contains("system:user:create"));
        jdbc.update("UPDATE sys_menu SET status = 1 WHERE id = 11");
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = 1 AND menu_id = 12");
        assertFalse(access.codes(1).contains("system:user:create"));
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id = 1 AND menu_id = 11");
        assertTrue(access.menus(1).isEmpty());
    }
}
