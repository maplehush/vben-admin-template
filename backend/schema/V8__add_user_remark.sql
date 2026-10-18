ALTER TABLE sys_user
    ADD COLUMN remark VARCHAR(500) NULL COMMENT '用户备注' AFTER real_name;
