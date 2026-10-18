package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.modules.system.auth.mapper.AuthLogMapper;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.service.ICaptchaService;
import com.maplehush.admin.modules.system.auth.service.ILoginService;
import com.maplehush.admin.modules.system.auth.service.ISessionService;
import com.maplehush.admin.modules.system.auth.service.RedisRateLimiter;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.time.Duration;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements ILoginService {

    private final AuthUserMapper users;
    private final AuthLogMapper logs;
    private final PasswordEncoder passwords;
    private final ICaptchaService captchas;
    private final RedisRateLimiter limits;
    private final ISessionService sessions;
    private final String dummyHash;

    public LoginServiceImpl(
            AuthUserMapper users,
            AuthLogMapper logs,
            PasswordEncoder passwords,
            ICaptchaService captchas,
            RedisRateLimiter limits,
            ISessionService sessions) {
        this.users = users;
        this.logs = logs;
        this.passwords = passwords;
        this.captchas = captchas;
        this.limits = limits;
        this.sessions = sessions;
        this.dummyHash = passwords.encode("not-a-real-account-password");
    }

    @Override
    public ISessionService.TokenPair login(
            String username,
            String password,
            String captchaId,
            String captchaCode,
            String ipAddress,
            String userAgent) {
        String usernameKey = "auth:login:user:" + username.toLowerCase(Locale.ROOT);
        try {
            limits.check(usernameKey, 10, Duration.ofMinutes(5));
            limits.check("auth:login:ip:" + ipAddress, 30, Duration.ofMinutes(5));
            captchas.consume(captchaId, captchaCode);

            SysUser user = users.findForLogin(username);
            if (!passwords.matches(password, user == null ? dummyHash : user.getPasswordHash())) {
                throw ApiException.invalidCredentials();
            }
            if (user == null) {
                throw ApiException.invalidCredentials();
            }
            if (user.getStatus() != 1) {
                throw ApiException.disabledUser();
            }

            ISessionService.TokenPair tokens = sessions.create(user.getId(), ipAddress, truncate(userAgent, 500));
            recordAttempt(user.getId(), username, true, null, ipAddress, userAgent);
            limits.reset(usernameKey);
            return tokens;
        } catch (ApiException exception) {
            recordAttempt(null, username, false, Integer.toString(exception.code()), ipAddress, userAgent);
            throw exception;
        }
    }

    private void recordAttempt(
            Long userId, String username, boolean success, String reason, String ipAddress, String userAgent) {
        logs.insertLoginAttempt(userId, username, success, reason, ipAddress, truncate(userAgent, 500));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
