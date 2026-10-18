package com.maplehush.admin.modules.system.dept.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.maplehush.admin.modules.system.auth.service.TestMapperFactory;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.dto.DeptQueryDTO;
import com.maplehush.admin.modules.system.dept.mapper.SysDeptMapper;
import com.maplehush.admin.modules.system.dept.service.impl.SysDeptQueryServiceImpl;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.impl.ManagementPermissionServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SysDeptQueryServiceTest {
    private JdbcTemplate jdbc;
    private ISysDeptQueryService queries;

    @BeforeEach
    void setUp() throws Exception {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:tree_query_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE sys_dept (id BIGINT PRIMARY KEY, parent_id BIGINT, dept_code VARCHAR(64),
                  dept_name VARCHAR(128), sort_order INT, status TINYINT, remark VARCHAR(500),
                  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_role (id BIGINT PRIMARY KEY, role_code VARCHAR(64), status TINYINT, data_scope_type VARCHAR(32))");
        jdbc.execute(
                "CREATE TABLE sys_menu (id BIGINT PRIMARY KEY, parent_id BIGINT, auth_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_menu (role_id BIGINT, menu_id BIGINT)");
        jdbc.execute(
                "CREATE TABLE sys_permission (id BIGINT PRIMARY KEY, permission_code VARCHAR(128), status TINYINT)");
        jdbc.execute("CREATE TABLE sys_role_permission (role_id BIGINT, permission_id BIGINT)");
        jdbc.update("INSERT INTO sys_role VALUES (1,'super_admin',1,'ALL'), (2,'dept_reader',1,'SELF')");
        jdbc.update("INSERT INTO sys_user_role VALUES (1,1), (2,2)");
        jdbc.update("""
                INSERT INTO sys_dept (id,parent_id,dept_code,dept_name,sort_order,status) VALUES
                (10,NULL,'root','Root',20,1), (11,10,'child','Child',5,0),
                (12,10,'active','Active',1,1), (20,NULL,'other','Other',10,1)
                """);
        jdbc.update("INSERT INTO sys_permission VALUES (200,'system:dept:list',1)");
        jdbc.update("INSERT INTO sys_role_permission VALUES (2,200)");
        queries = new SysDeptQueryServiceImpl(
                TestMapperFactory.create(source).getMapper(SysDeptMapper.class),
                new ManagementPermissionServiceImpl(
                        TestMapperFactory.create(source).getMapper(ManagementPermissionMapper.class)));
    }

    @Test
    void departmentTreeIsSortedAndFilterKeepsAncestors() {
        var all = queries.departments(1, new DeptQueryDTO(null));
        assertEquals("20", all.getFirst().id());
        assertEquals("12", all.get(1).children().getFirst().id());
        assertNotNull(all.get(1).createTime());
        var disabled = queries.departments(1, new DeptQueryDTO("DISABLED"));
        assertEquals(1, disabled.size());
        assertEquals("10", disabled.getFirst().id());
        assertEquals("11", disabled.getFirst().children().getFirst().id());
        assertEquals(1, disabled.getFirst().children().size());
        jdbc.update("UPDATE sys_dept SET remark = '运营部' WHERE id = 11");
        assertEquals(
                "运营部",
                queries.departments(1, new DeptQueryDTO(null))
                        .get(1)
                        .children()
                        .get(1)
                        .remark());
    }

    @Test
    void departmentDetailAndPermissionsUseSeparateCodes() {
        assertEquals("Child", queries.department(1, 11).deptName());
        assertEquals(
                40400,
                assertThrows(ApiException.class, () -> queries.department(1, 999))
                        .code());
        assertEquals(1, queries.departments(2, new DeptQueryDTO("DISABLED")).size());
        assertEquals(
                40300,
                assertThrows(ApiException.class, () -> queries.department(2, 10))
                        .code());
        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> queries.departments(1, new DeptQueryDTO("UNKNOWN")))
                        .code());
    }
}
