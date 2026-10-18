package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.modules.system.auth.mapper.AuthUserMapper;
import com.maplehush.admin.modules.system.auth.service.IBootstrapAdminService;
import com.maplehush.admin.modules.system.auth.service.InitialPasswordPolicy;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BootstrapAdminServiceImpl implements IBootstrapAdminService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.-]{3,64}");

    private final AuthUserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transaction;

    public BootstrapAdminServiceImpl(
            AuthUserMapper users, PasswordEncoder passwordEncoder, PlatformTransactionManager transactionManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public long createFirstAdmin(String username) {
        Long userId = createFirstAdmin(username, false);
        if (userId == null) {
            throw new IllegalStateException("Administrator bootstrap did not complete");
        }
        return userId;
    }

    @Override
    public Long createFirstAdminIfEmpty(String username) {
        return createFirstAdmin(username, true);
    }

    private Long createFirstAdmin(String username, boolean skipExisting) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must be 3-64 letters, digits, underscores, dots or hyphens");
        }

        Long userId = transaction.execute(status -> {
            // Lock the seeded role to serialize simultaneous bootstrap attempts.
            Long roleId = users.lockEnabledSuperAdminRole();
            if (roleId == null) {
                throw new IllegalStateException("The enabled super_admin role is missing");
            }
            if (users.selectCount(null) != 0) {
                if (skipExisting) {
                    return null;
                }
                throw new IllegalStateException("Administrator bootstrap is only allowed before any user exists");
            }

            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(InitialPasswordPolicy.DEFAULT_PASSWORD));
            user.setRealName("超级管理员");
            user.setStatus(1);
            user.setMustChangePassword(1);
            users.insert(user);
            Long createdId = user.getId();
            if (createdId == null) {
                throw new IllegalStateException("Administrator was not created");
            }
            users.assignRole(createdId, roleId);
            return createdId;
        });
        return userId;
    }
}
