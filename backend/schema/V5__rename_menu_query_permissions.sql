-- V4 将资源管理入口改为菜单管理；查询与详情属于独立 API 权限，需同步改名。
-- 保留原 ID 与 sys_role_permission 授权关系。
UPDATE sys_permission
SET permission_key = REPLACE(permission_key, 'perm.system.resource.', 'perm.system.menu.'),
    permission_code = REPLACE(permission_code, 'system:resource:', 'system:menu:'),
    title_key = REPLACE(title_key, '资源', '菜单')
WHERE permission_key IN ('perm.system.resource.list', 'perm.system.resource.view');
