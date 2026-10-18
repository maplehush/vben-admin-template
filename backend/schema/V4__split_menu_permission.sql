-- V1-V3 的 sys_resource 拆分为导航菜单与操作权限；保留原 ID 和已有角色授权。
-- Flyway 版本不可回改。生产部署须先备份，再在维护窗口执行。

CREATE TABLE sys_menu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    menu_type VARCHAR(16) NOT NULL,
    menu_key VARCHAR(128) NOT NULL,
    route_name VARCHAR(128) NULL,
    route_path VARCHAR(255) NULL,
    component VARCHAR(255) NULL,
    redirect_path VARCHAR(255) NULL,
    auth_code VARCHAR(128) NULL,
    title_key VARCHAR(255) NOT NULL,
    icon VARCHAR(128) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    route_meta JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_menu_key (menu_key),
    UNIQUE KEY uk_sys_menu_route_name (route_name),
    UNIQUE KEY uk_sys_menu_route_path (route_path),
    UNIQUE KEY uk_sys_menu_auth_code (auth_code),
    KEY idx_sys_menu_parent_sort (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_menu
    (id, parent_id, menu_type, menu_key, route_name, route_path, component,
     redirect_path, auth_code, title_key, icon, sort_order, route_meta, status, create_time, update_time)
SELECT id, parent_id, resource_type, resource_key, route_name, route_path, component,
       redirect_path, permission_code, title_key, icon, sort_order, route_meta, status, create_time, update_time
FROM sys_resource WHERE resource_type IN ('CATALOG', 'MENU', 'BUTTON') ORDER BY id;

ALTER TABLE sys_menu ADD CONSTRAINT fk_sys_menu_parent
    FOREIGN KEY (parent_id) REFERENCES sys_menu (id);

CREATE TABLE sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    menu_id BIGINT NULL,
    permission_key VARCHAR(128) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    title_key VARCHAR(255) NOT NULL,
    icon VARCHAR(128) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    route_meta JSON NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_key (permission_key),
    UNIQUE KEY uk_sys_permission_code (permission_code),
    KEY idx_sys_permission_menu_sort (menu_id, sort_order),
    CONSTRAINT fk_sys_permission_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_permission
    (id, menu_id, permission_key, permission_code, title_key,
     icon, sort_order, route_meta, status, create_time, update_time)
SELECT id, parent_id, resource_key, permission_code, title_key,
       icon, sort_order, route_meta, status, create_time, update_time
FROM sys_resource WHERE resource_type = 'API' ORDER BY id;

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, menu_id),
    KEY idx_sys_role_menu_menu (menu_id, role_id),
    CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    KEY idx_sys_role_permission_permission (permission_id, role_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT rr.role_id, rr.resource_id, rr.create_time
FROM sys_role_resource rr JOIN sys_menu m ON m.id = rr.resource_id;

INSERT INTO sys_role_permission (role_id, permission_id, create_time)
SELECT rr.role_id, rr.resource_id, rr.create_time
FROM sys_role_resource rr JOIN sys_permission p ON p.id = rr.resource_id;

-- 旧资源管理入口转为菜单管理；原角色授权 ID 不变。
UPDATE sys_menu SET menu_key = 'menu.system.menu', route_name = 'SystemMenu',
    route_path = '/system/menu', component = '/system/menu/list',
    title_key = 'system.menu.title'
WHERE menu_key = 'menu.system.resource';

UPDATE sys_menu
SET menu_key = REPLACE(menu_key, 'perm.system.resource.', 'perm.system.menu.'),
    auth_code = REPLACE(auth_code, 'system:resource:', 'system:menu:'),
    title_key = REPLACE(title_key, '资源', '菜单')
WHERE menu_key LIKE 'perm.system.resource.%';

INSERT INTO sys_menu
    (parent_id, menu_type, menu_key, route_name, route_path, component,
     title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.system.permission', 'SystemPermission',
       '/system/permission', '/system/permission/list',
       'system.permission.title', 'carbon:security', 45
FROM sys_menu WHERE menu_key = 'menu.system';

INSERT INTO sys_permission
    (menu_id, permission_key, permission_code, title_key, sort_order)
SELECT m.id, x.permission_key, x.permission_code, x.title_key, x.sort_order
FROM sys_menu m JOIN (
    SELECT 'perm.system.permission.list' AS permission_key,
           'system:permission:list' AS permission_code, '查询权限列表' AS title_key, 10 AS sort_order
    UNION ALL SELECT 'perm.system.permission.view', 'system:permission:view', '查看权限详情', 20
) x WHERE m.menu_key = 'menu.system.permission';

INSERT INTO sys_menu
    (parent_id, menu_type, menu_key, auth_code, title_key, sort_order)
SELECT m.id, 'BUTTON', x.menu_key, x.auth_code, x.title_key, x.sort_order
FROM sys_menu m JOIN (
    SELECT 'perm.system.permission.create' AS menu_key,
           'system:permission:create' AS auth_code, '新增权限' AS title_key, 30 AS sort_order
    UNION ALL SELECT 'perm.system.permission.update', 'system:permission:update', '编辑权限', 40
    UNION ALL SELECT 'perm.system.permission.delete', 'system:permission:delete', '删除权限', 50
) x WHERE m.menu_key = 'menu.system.permission';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'super_admin' AND
    (m.menu_key = 'menu.system.permission' OR m.menu_key LIKE 'perm.system.permission.%');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin' AND p.permission_key LIKE 'perm.system.permission.%';

-- MySQL DDL 不完全事务化，部署流程须确保迁移备份可恢复。
DROP TABLE sys_role_resource;
DROP TABLE sys_resource;
