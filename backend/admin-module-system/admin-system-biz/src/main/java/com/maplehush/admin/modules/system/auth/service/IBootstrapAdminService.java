package com.maplehush.admin.modules.system.auth.service;

/** 初始化超级管理员账号 */
public interface IBootstrapAdminService {
    public long createFirstAdmin(String username);

    /** 空库创建初始管理员；已有用户时不修改任何账号，返回 null */
    public Long createFirstAdminIfEmpty(String username);
}
