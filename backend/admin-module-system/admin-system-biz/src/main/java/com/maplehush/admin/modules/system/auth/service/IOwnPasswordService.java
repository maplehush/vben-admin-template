package com.maplehush.admin.modules.system.auth.service;

/** 当前用户密码修改 */
public interface IOwnPasswordService {
    public void change(long userId, String oldPassword, String newPassword);
}
