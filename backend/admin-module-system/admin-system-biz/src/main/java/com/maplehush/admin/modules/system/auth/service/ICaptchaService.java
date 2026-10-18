package com.maplehush.admin.modules.system.auth.service;

import com.maplehush.admin.modules.system.auth.vo.CaptchaChallengeVO;

/** 登录验证码生成和校验 */
public interface ICaptchaService {
    public CaptchaChallengeVO issue(String ipAddress);

    public void consume(String id, String response);
}
