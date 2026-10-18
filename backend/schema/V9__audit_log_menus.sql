-- 审计表由 V1 创建，本迁移只增加查询页面和授权项。
INSERT INTO sys_menu
    (parent_id, menu_type, menu_key, route_name, route_path, component,
     title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.system.login-log', 'SystemLoginLog',
       '/system/login-log', '/system/login-log/list',
       'system.loginLog.title', 'carbon:login', 60
FROM sys_menu WHERE menu_key = 'menu.system';

INSERT INTO sys_menu
    (parent_id, menu_type, menu_key, route_name, route_path, component,
     title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.system.operation-log', 'SystemOperationLog',
       '/system/operation-log', '/system/operation-log/list',
       'system.operationLog.title', 'carbon:document-tasks', 70
FROM sys_menu WHERE menu_key = 'menu.system';

INSERT INTO sys_permission
    (menu_id, permission_key, permission_code, title_key, sort_order)
SELECT m.id, 'perm.system.login-log.list', 'system:login-log:list',
       '查询登录日志', 10
FROM sys_menu m WHERE m.menu_key = 'menu.system.login-log';

INSERT INTO sys_permission
    (menu_id, permission_key, permission_code, title_key, sort_order)
SELECT m.id, 'perm.system.operation-log.list', 'system:operation-log:list',
       '查询操作日志', 10
FROM sys_menu m WHERE m.menu_key = 'menu.system.operation-log';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'super_admin' AND m.menu_key IN
    ('menu.system.login-log', 'menu.system.operation-log');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin' AND p.permission_key IN
    ('perm.system.login-log.list', 'perm.system.operation-log.list');
