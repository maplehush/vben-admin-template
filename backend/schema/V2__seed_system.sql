-- 基础模板种子数据。Flyway 仅执行一次；不要预置账号或明文密码。
-- 依赖 V1__init.sql。系统管理页面组件须由前端接入时实现。

INSERT INTO sys_role (role_code, role_name, data_scope_type, status, description)
VALUES ('super_admin', '超级管理员', 'ALL', 1, '模板内置角色；服务端保留全部权限，不允许停用或删除');

-- 根目录。route_meta 仅存已批准的展示属性。
INSERT INTO sys_resource
    (resource_type, resource_key, route_name, route_path, redirect_path, title_key, icon, sort_order, route_meta)
VALUES
    ('CATALOG', 'menu.dashboard', 'Dashboard', '/dashboard', '/dashboard/workspace', 'page.dashboard.title', 'lucide:layout-dashboard', -1, JSON_OBJECT('order', -1)),
    ('CATALOG', 'menu.system', 'System', '/system', '/system/user', 'system.title', 'carbon:settings', 100, JSON_OBJECT('order', 100));

-- 已在 Vben web-antd 源码中存在的工作台页面。
INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, route_name, route_path, component, title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.dashboard.workspace', 'Workspace', '/dashboard/workspace',
       '/dashboard/workspace/index', 'page.dashboard.workspace', 'carbon:workspace', 10
FROM sys_resource WHERE resource_key = 'menu.dashboard';

-- 四个系统管理页面由模板前端接入阶段实现，路径与此处保持一致。
INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, route_name, route_path, component, title_key, icon, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.route_name, x.route_path,
       x.component, x.title_key, x.icon, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'MENU' AS resource_type, 'menu.system.user' AS resource_key,
           'SystemUser' AS route_name, '/system/user' AS route_path,
           '/system/user/list' AS component, 'system.user.title' AS title_key,
           'carbon:user' AS icon, 10 AS sort_order
    UNION ALL SELECT 'MENU', 'menu.system.role', 'SystemRole', '/system/role',
           '/system/role/list', 'system.role.title', 'carbon:user-role', 20
    UNION ALL SELECT 'MENU', 'menu.system.dept', 'SystemDept', '/system/dept',
           '/system/dept/list', 'system.dept.title', 'carbon:tree-view', 30
    UNION ALL SELECT 'MENU', 'menu.system.resource', 'SystemResource', '/system/resource',
           '/system/resource/list', 'system.resource.title', 'carbon:menu', 40
) AS x
WHERE p.resource_key = 'menu.system';

-- 无独立按钮的列表/详情查询以 API 资源登记；其余操作以 BUTTON 登记。
-- permission_code 与 OpenAPI x-permission-code、后端 @PreAuthorize 使用同一字符串。
INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, permission_code, title_key, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.permission_code, x.title_key, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'API' AS resource_type, 'perm.system.user.list' AS resource_key,
           'system:user:list' AS permission_code, '查询用户列表' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'API', 'perm.system.user.view', 'system:user:view', '查看用户详情', 20
    UNION ALL SELECT 'BUTTON', 'perm.system.user.create', 'system:user:create', '新增用户', 30
    UNION ALL SELECT 'BUTTON', 'perm.system.user.update', 'system:user:update', '编辑用户', 40
    UNION ALL SELECT 'BUTTON', 'perm.system.user.status', 'system:user:status', '启停用户', 50
    UNION ALL SELECT 'BUTTON', 'perm.system.user.grant', 'system:user:grant', '分配用户角色', 60
    UNION ALL SELECT 'BUTTON', 'perm.system.user.reset-password', 'system:user:reset-password', '重置用户密码', 70
) AS x
WHERE p.resource_key = 'menu.system.user';

INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, permission_code, title_key, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.permission_code, x.title_key, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'API' AS resource_type, 'perm.system.role.list' AS resource_key,
           'system:role:list' AS permission_code, '查询角色列表' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'API', 'perm.system.role.view', 'system:role:view', '查看角色详情', 20
    UNION ALL SELECT 'BUTTON', 'perm.system.role.create', 'system:role:create', '新增角色', 30
    UNION ALL SELECT 'BUTTON', 'perm.system.role.update', 'system:role:update', '编辑角色', 40
    UNION ALL SELECT 'BUTTON', 'perm.system.role.delete', 'system:role:delete', '删除角色', 50
    UNION ALL SELECT 'BUTTON', 'perm.system.role.grant', 'system:role:grant', '配置角色资源', 60
    UNION ALL SELECT 'BUTTON', 'perm.system.role.data-scope', 'system:role:data-scope', '配置角色数据范围', 70
) AS x
WHERE p.resource_key = 'menu.system.role';

INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, permission_code, title_key, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.permission_code, x.title_key, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'API' AS resource_type, 'perm.system.dept.list' AS resource_key,
           'system:dept:list' AS permission_code, '查询部门树' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'API', 'perm.system.dept.view', 'system:dept:view', '查看部门详情', 20
    UNION ALL SELECT 'BUTTON', 'perm.system.dept.create', 'system:dept:create', '新增部门', 30
    UNION ALL SELECT 'BUTTON', 'perm.system.dept.update', 'system:dept:update', '编辑部门', 40
    UNION ALL SELECT 'BUTTON', 'perm.system.dept.delete', 'system:dept:delete', '删除部门', 50
) AS x
WHERE p.resource_key = 'menu.system.dept';

INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, permission_code, title_key, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.permission_code, x.title_key, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'API' AS resource_type, 'perm.system.resource.list' AS resource_key,
           'system:resource:list' AS permission_code, '查询资源树' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'API', 'perm.system.resource.view', 'system:resource:view', '查看资源详情', 20
    UNION ALL SELECT 'BUTTON', 'perm.system.resource.create', 'system:resource:create', '新增资源', 30
    UNION ALL SELECT 'BUTTON', 'perm.system.resource.update', 'system:resource:update', '编辑资源', 40
    UNION ALL SELECT 'BUTTON', 'perm.system.resource.delete', 'system:resource:delete', '删除资源', 50
) AS x
WHERE p.resource_key = 'menu.system.resource';

-- 为后台授权界面留下一份可见的初始关系；super_admin 的固有全权仍由应用层保证。
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, s.id
FROM sys_role AS r
CROSS JOIN sys_resource AS s
WHERE r.role_code = 'super_admin';
