package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.mapper.AuthenticatedSessionState;
import com.maplehush.admin.modules.system.auth.service.IAccessTokenService;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** 将有效访问令牌与数据库会话、账号状态逐次核对。 */
@Service
public class CurrentUserServiceImpl implements ICurrentUserService {

    private final Clock clock;
    private final AuthUserMapper users;
    private final IAccessTokenService tokens;

    public CurrentUserServiceImpl(AuthUserMapper users, IAccessTokenService tokens, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    public CurrentUser authenticate(String bearerToken) {
        IAccessTokenService.TokenIdentity identity = tokens.verify(bearerToken);
        AuthenticatedSessionState row = users.findAuthenticatedUser(identity.userId(), identity.sessionId());
        if (row == null) {
            throw ApiException.invalidAccessToken();
        }
        if (row.status() != 1) {
            throw ApiException.disabledUser();
        }
        if (row.revokeTime() != null || !row.expireTime().isAfter(DateUtils.now(clock))) {
            throw ApiException.invalidAccessToken();
        }
        return new CurrentUser(row.userId(), row.sessionId(), row.mustChangePassword());
    }
}
