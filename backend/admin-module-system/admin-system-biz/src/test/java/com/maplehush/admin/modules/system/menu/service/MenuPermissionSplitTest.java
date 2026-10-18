package com.maplehush.admin.modules.system.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.impl.OperationAuditServiceImpl;
import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.menu.mapper.SysMenuMapper;
import com.maplehush.admin.modules.system.menu.service.impl.SysMenuQueryServiceImpl;
import com.maplehush.admin.modules.system.permission.dto.PermissionQueryDTO;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.mapper.SysPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.ISysPermissionService;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import com.maplehush.admin.modules.system.permission.service.impl.SysPermissionServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import tools.jackson.databind.ObjectMapper;

class MenuPermissionSplitTest {
    private JdbcTemplate jdbc;
    private ISysMenuQueryService menus;
    private ISysPermissionService apiPermissions;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:menu_permission_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, menu_type VARCHAR(16),
                  menu_key VARCHAR(128), route_name VARCHAR(128), route_path VARCHAR(255),
                  component VARCHAR(255), redirect_path VARCHAR(255), auth_code VARCHAR(128),
                  title_key VARCHAR(255), icon VARCHAR(128), sort_order INT, route_meta VARCHAR(1000),
                  status TINYINT, create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE sys_permission (id BIGINT AUTO_INCREMENT PRIMARY KEY, menu_id BIGINT,
                  permission_key VARCHAR(128) UNIQUE, permission_code VARCHAR(128) UNIQUE,
                  title_key VARCHAR(255), sort_order INT, status TINYINT,
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute(
                "CREATE TABLE sys_operation_log (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT, action_code VARCHAR(128), target_type VARCHAR(64), target_id VARCHAR(128), success TINYINT, request_id VARCHAR(64), ip_address VARCHAR(45))");
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'viewer',1,'SELF')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update("""
                INSERT INTO sys_menu (id,parent_id,menu_type,menu_key,route_name,route_path,
                  component,auth_code,title_key,sort_order,status) VALUES
                (10,NULL,'CATALOG','menu.system','System','/system',NULL,NULL,'System',0,1),
                (11,10,'MENU','menu.user','User','/system/user','/system/user/list',NULL,'User',1,1),
                (12,11,'BUTTON','button.user.create',NULL,NULL,NULL,'system:user:create','Create',2,1)
                """);
        var access = new ManagementPermissionServiceImpl(
                TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class));
        menus = new SysMenuQueryServiceImpl(
                TestMapperFactory.create(source).getMapper(SysMenuMapper.class), access, new ObjectMapper());
        apiPermissions = new SysPermissionServiceImpl(
                TestMapperFactory.create(source).getMapper(SysPermissionMapper.class),
                access,
                new OperationAuditServiceImpl(TestMapperFactory.create(source).getMapper(OperationAuditMapper.class)),
                new DataSourceTransactionManager(source));
    }

    @Test
    void menuTreeContainsButtonsAndApiPermissionsRemainSeparate() {
        assertEquals(
                "BUTTON",
                menus.tree(1)
                        .getFirst()
                        .children()
                        .getFirst()
                        .children()
                        .getFirst()
                        .type());
        assertEquals("system:user:create", menus.detail(1, 12).authCode());
        assertEquals(
                0, apiPermissions.list(1, new PermissionQueryDTO(1, 20, null)).total());
        assertEquals(
                40300, assertThrows(ApiException.class, () -> menus.tree(2)).code());
    }

    @Test
    void menuDetailRetainsBadgeAndVisibilitySettingsForEditing() {
        jdbc.update(
                "UPDATE sys_menu SET route_meta = ? WHERE id = 11",
                "{\"badgeType\":\"dot\",\"badgeVariants\":\"success\",\"hideInMenu\":true,\"affixTab\":false}");
        var meta = menus.detail(1, 11).routeMeta();
        assertEquals("dot", meta.get("badgeType"));
        assertEquals("success", meta.get("badgeVariants"));
        assertEquals(true, meta.get("hideInMenu"));
        assertEquals(false, meta.get("affixTab"));
    }

    @Test
    void apiCodeCannotDuplicateButtonCodeAndGrantedPermissionCannotBeDeleted() {
        var data = new ISysPermissionService.PermissionData(11L, "Export", 10, "ENABLED");
        assertEquals(
                40901,
                assertThrows(
                                ApiException.class,
                                () -> apiPermissions.create(1, "api.export", "system:user:create", data))
                        .code());
        long id = apiPermissions.create(1, "api.export", "system:user:export", data);
        assertEquals("system:user:export", apiPermissions.detail(1, id).permissionCode());
        jdbc.update("INSERT INTO sys_role_permission VALUES (2,?)", id);
        assertEquals(
                40901,
                assertThrows(ApiException.class, () -> apiPermissions.delete(1, id))
                        .code());
        jdbc.update("DELETE FROM sys_role_permission WHERE permission_id = ?", id);
        apiPermissions.delete(1, id);
        assertEquals(
                0, apiPermissions.list(1, new PermissionQueryDTO(1, 20, null)).total());
    }
}
