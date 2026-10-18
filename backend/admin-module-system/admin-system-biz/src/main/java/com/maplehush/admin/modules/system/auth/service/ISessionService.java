package com.maplehush.admin.modules.system.auth.service;

/** 登录会话创建和续期 */
public interface ISessionService {
    public TokenPair create(long userId, String ipAddress, String userAgent);

    public TokenPair refresh(String oldToken);

    public void revoke(String token);

    public record TokenPair(String accessToken, String refreshToken) {}
}
