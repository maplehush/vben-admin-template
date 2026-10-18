package com.maplehush.admin.modules.system.auth.vo;

/** 验证码挑战响应视图 */
public record CaptchaChallengeVO(String captchaId, String imageData, long expiresInSeconds) {}
