package com.maplehush.admin.modules.system.auth.service;

/** 当前登录用户识别 */
public interface ICurrentUserService {
    public CurrentUser authenticate(String bearerToken);

    public record CurrentUser(long userId, long sessionId, boolean mustChangePassword) {}
}
