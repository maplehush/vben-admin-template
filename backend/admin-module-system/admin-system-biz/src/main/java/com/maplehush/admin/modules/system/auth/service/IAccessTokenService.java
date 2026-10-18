package com.maplehush.admin.modules.system.auth.service;

/** 访问令牌签发和校验 */
public interface IAccessTokenService {
    public TokenIdentity verify(String token);

    public String issue(long userId, long sessionId);

    public record TokenIdentity(long userId, long sessionId) {}
}
