package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.IOwnPasswordService;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 修改本人密码并在同一事务中撤销已有会话。 */
@Service
public class OwnPasswordServiceImpl implements IOwnPasswordService {

    private final Clock clock;
    private final AuthUserMapper users;
    private final SysSessionMapper sessions;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final IOperationAuditService audit;

    public OwnPasswordServiceImpl(
            AuthUserMapper users,
            SysSessionMapper sessions,
            PasswordEncoder encoder,
            PlatformTransactionManager transactionManager,
            IOperationAuditService audit,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.transaction = new TransactionTemplate(transactionManager);
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public void change(long userId, String oldPassword, String newPassword) {
        if (newPassword == null
                || newPassword.length() < 8
                || newPassword.length() > 128
                || newPassword.equals(oldPassword)) {
            throw ApiException.invalidNewPassword();
        }
        transaction.executeWithoutResult(status -> {
            String hash = users.lockPasswordHash(userId);
            if (hash == null || !encoder.matches(oldPassword, hash)) {
                throw ApiException.wrongOldPassword();
            }
            LocalDateTime now = DateUtils.now(clock);
            users.changePassword(userId, encoder.encode(newPassword), now);
            sessions.revokeAllForUser(userId, now);
            audit.record(userId, "user:password", AuditTargetType.USER, userId);
        });
    }
}
