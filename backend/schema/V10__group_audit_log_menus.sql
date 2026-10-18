-- 审计页面归入系统管理下的日志管理目录；保留权限码和原菜单 ID。
INSERT INTO sys_menu
    (parent_id, menu_type, menu_key, route_name, route_path,
     title_key, icon, sort_order)
SELECT id, 'CATALOG', 'menu.system.log', 'SystemLog', '/system/log',
       'system.log.title', 'carbon:document', 55
FROM sys_menu WHERE menu_key = 'menu.system';

UPDATE sys_menu SET
    parent_id = (SELECT id FROM (SELECT id FROM sys_menu
                 WHERE menu_key = 'menu.system.log') AS log_menu),
    route_path = '/system/log/login',
    component = '/system/log/login/list',
    sort_order = 10
WHERE menu_key = 'menu.system.login-log';

UPDATE sys_menu SET
    parent_id = (SELECT id FROM (SELECT id FROM sys_menu
                 WHERE menu_key = 'menu.system.log') AS log_menu),
    route_path = '/system/log/operation',
    component = '/system/log/operation/list',
    sort_order = 20
WHERE menu_key = 'menu.system.operation-log';

-- 已获任一日志页面的角色补齐父目录授权，避免授权树出现孤立子项。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT granted.role_id, catalog.id
FROM sys_role_menu granted
JOIN sys_menu page ON page.id = granted.menu_id
JOIN sys_menu catalog ON catalog.menu_key = 'menu.system.log'
WHERE page.menu_key IN ('menu.system.login-log', 'menu.system.operation-log')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu existing
      WHERE existing.role_id = granted.role_id AND existing.menu_id = catalog.id
  );
