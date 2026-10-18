-- 一次性历史数据转换。仅用于切换前按 UTC 存储 DATETIME 的现有数据库。
-- 执行前停掉写入服务并完整备份；本脚本不可重复执行。
-- 全部业务时间列加 8 小时，Flyway 的 installed_on 为 TIMESTAMP，不参与转换。
SET SESSION time_zone = '+08:00';
START TRANSACTION;

UPDATE sys_dept SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR), update_time = DATE_ADD(update_time, INTERVAL 8 HOUR);
UPDATE sys_login_log SET occur_time = DATE_ADD(occur_time, INTERVAL 8 HOUR);
UPDATE sys_menu SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR), update_time = DATE_ADD(update_time, INTERVAL 8 HOUR);
UPDATE sys_operation_log SET occur_time = DATE_ADD(occur_time, INTERVAL 8 HOUR);
UPDATE sys_permission SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR), update_time = DATE_ADD(update_time, INTERVAL 8 HOUR);
UPDATE sys_role SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR), update_time = DATE_ADD(update_time, INTERVAL 8 HOUR);
UPDATE sys_role_menu SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR);
UPDATE sys_role_permission SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR);
UPDATE sys_session SET expire_time = DATE_ADD(expire_time, INTERVAL 8 HOUR), revoke_time = DATE_ADD(revoke_time, INTERVAL 8 HOUR), create_time = DATE_ADD(create_time, INTERVAL 8 HOUR);
UPDATE sys_user SET password_change_time = DATE_ADD(password_change_time, INTERVAL 8 HOUR), last_login_time = DATE_ADD(last_login_time, INTERVAL 8 HOUR), create_time = DATE_ADD(create_time, INTERVAL 8 HOUR), update_time = DATE_ADD(update_time, INTERVAL 8 HOUR);
UPDATE sys_user_role SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR);

COMMIT;
