package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.auth.config.AuthProperties;
import com.maplehush.admin.modules.system.auth.entity.SysSession;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.mapper.RefreshSessionState;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.IAccessTokenService;
import com.maplehush.admin.modules.system.auth.service.ISessionService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 管理刷新令牌对应的持久会话。轮换令牌时锁定会话行，避免同一令牌并发重放。 */
@Service
public class SessionServiceImpl implements ISessionService {

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final SysSessionMapper sessions;
    private final AuthUserMapper users;
    private final TransactionTemplate transaction;
    private final IAccessTokenService accessTokens;
    private final AuthProperties properties;

    public SessionServiceImpl(
            SysSessionMapper sessions,
            AuthUserMapper users,
            PlatformTransactionManager transactionManager,
            IAccessTokenService accessTokens,
            AuthProperties properties,
            Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.transaction = new TransactionTemplate(transactionManager);
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public TokenPair create(long userId, String ipAddress, String userAgent) {
        String refreshToken = randomToken();
        LocalDateTime expiresAt = DateUtils.after(clock, properties.refreshTtl());
        Long sessionId = transaction.execute(status -> {
            SysSession session = new SysSession();
            session.setUserId(userId);
            session.setRefreshTokenHash(hash(refreshToken));
            session.setDeviceInfo(userAgent);
            session.setIpAddress(ipAddress);
            session.setExpireTime(expiresAt);
            sessions.insert(session);
            users.recordLogin(userId, DateUtils.now(clock));
            return session.getId();
        });
        if (sessionId == null) {
            throw new IllegalStateException("Could not create login session");
        }
        return new TokenPair(accessTokens.issue(userId, sessionId), refreshToken);
    }

    @Override
    public TokenPair refresh(String oldToken) {
        if (oldToken == null || oldToken.isBlank()) {
            throw ApiException.invalidRefreshToken();
        }
        String newToken = randomToken();
        RefreshSessionState session = transaction.execute(status -> {
            RefreshSessionState current = sessions.lockByRefreshHash(hash(oldToken));
            if (current == null) {
                throw ApiException.invalidRefreshToken();
            }
            if (current.revokeTime() != null
                    || current.status() != 1
                    || !current.expireTime().isAfter(DateUtils.now(clock))) {
                throw ApiException.invalidRefreshToken();
            }
            SysSession updated = new SysSession();
            updated.setId(current.id());
            updated.setRefreshTokenHash(hash(newToken));
            updated.setExpireTime(DateUtils.after(clock, properties.refreshTtl()));
            sessions.updateById(updated);
            return current;
        });
        if (session == null) {
            throw ApiException.invalidRefreshToken();
        }
        return new TokenPair(accessTokens.issue(session.userId(), session.id()), newToken);
    }

    @Override
    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            sessions.revokeByHash(hash(token), DateUtils.now(clock));
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
