-- 字典类型与字典项。固定启禁用状态与业务字典分开维护。
CREATE TABLE sys_dict (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(64) NOT NULL,
    dict_name VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    built_in TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dict_id BIGINT NOT NULL,
    item_value VARCHAR(128) NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    remark VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dict_item_value (dict_id, item_value),
    KEY idx_sys_dict_item_sort (dict_id, sort_order, id),
    CONSTRAINT fk_sys_dict_item_dict FOREIGN KEY (dict_id) REFERENCES sys_dict(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sys_dict (dict_code, dict_name, built_in) VALUES ('common_status', '启禁用状态', 1);
INSERT INTO sys_dict_item (dict_id, item_value, item_label, sort_order)
SELECT id, 'ENABLED', '已启用', 10 FROM sys_dict WHERE dict_code = 'common_status';
INSERT INTO sys_dict_item (dict_id, item_value, item_label, sort_order)
SELECT id, 'DISABLED', '已禁用', 20 FROM sys_dict WHERE dict_code = 'common_status';

INSERT INTO sys_menu (parent_id, menu_type, menu_key, route_name, route_path, component,
                      title_key, icon, sort_order)
SELECT id, 'MENU', 'menu.system.dict', 'SystemDict', '/system/dict',
       '/system/dict/list', 'system.dict.title', 'carbon:dictionary', 55
FROM sys_menu WHERE menu_key = 'menu.system';

INSERT INTO sys_permission (menu_id, permission_key, permission_code, title_key, sort_order)
SELECT m.id, CONCAT('perm.system.dict.', actions.action),
       CONCAT('system:dict:', actions.action), actions.title, actions.sort_order
FROM sys_menu m CROSS JOIN (
    SELECT 'list' action, '查询字典' title, 10 sort_order UNION ALL
    SELECT 'view', '查看字典', 20 UNION ALL
    SELECT 'create', '新增字典', 30 UNION ALL
    SELECT 'update', '修改字典', 40 UNION ALL
    SELECT 'delete', '删除字典', 50
) actions WHERE m.menu_key = 'menu.system.dict';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'super_admin' AND m.menu_key = 'menu.system.dict';
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'super_admin' AND p.permission_key LIKE 'perm.system.dict.%';
