package com.maplehush.admin.modules.system.auth.service;

/** 账号登录与验证码校验 */
public interface ILoginService {
    public ISessionService.TokenPair login(
            String username, String password, String captchaId, String captchaCode, String ipAddress, String userAgent);
}
