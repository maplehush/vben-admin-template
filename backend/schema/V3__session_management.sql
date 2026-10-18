-- 在线会话管理入口与权限。本人会话接口只要求登录，不需要此菜单权限。
INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, route_name, route_path, component,
     title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.system.session', 'SystemSession', '/system/session',
       '/system/session/list', 'system.session.title', 'carbon:connection-signal', 50
FROM sys_resource WHERE resource_key = 'menu.system';

INSERT INTO sys_resource
    (parent_id, resource_type, resource_key, permission_code, title_key, sort_order)
SELECT p.id, x.resource_type, x.resource_key, x.permission_code, x.title_key, x.sort_order
FROM sys_resource AS p
JOIN (
    SELECT 'API' AS resource_type, 'perm.system.session.list' AS resource_key,
           'system:session:list' AS permission_code, '查询在线会话' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'BUTTON', 'perm.system.session.revoke',
           'system:session:revoke', '强制下线会话', 20
) AS x
WHERE p.resource_key = 'menu.system.session';

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, s.id FROM sys_role AS r CROSS JOIN sys_resource AS s
WHERE r.role_code = 'super_admin' AND s.resource_key IN
    ('menu.system.session', 'perm.system.session.list', 'perm.system.session.revoke');
