package com.maplehush.admin.modules.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.IUserAccessService;
import com.maplehush.admin.modules.system.auth.service.InitialPasswordPolicy;
import com.maplehush.admin.modules.system.auth.vo.MenuRouteVO;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import com.maplehush.admin.modules.system.user.mapper.SysUserMapper;
import com.maplehush.admin.modules.system.user.service.ISysUserService;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 处理用户增改、状态、角色与密码重置，统一执行授权和数据范围校验。 */
@Service
public class SysUserServiceImpl implements ISysUserService {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.-]{3,64}");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private final Clock clock;
    private final SysUserMapper users;
    private final SysSessionMapper sessions;
    private final IManagementPermissionService permissions;
    private final PasswordEncoder encoder;
    private final IUserAccessService access;
    private final IOperationAuditService audit;
    private final TransactionTemplate transaction;

    public SysUserServiceImpl(
            SysUserMapper users,
            SysSessionMapper sessions,
            IManagementPermissionService permissions,
            PasswordEncoder encoder,
            IUserAccessService access,
            IOperationAuditService audit,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.permissions = permissions;
        this.encoder = encoder;
        this.access = access;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    public long create(long actor, CreateUser request) {
        var scope = permissions.userScope(actor, "system:user:create");
        if (request == null
                || request.username() == null
                || !USERNAME.matcher(request.username()).matches()
                || request.data() == null) throw ApiException.invalidRequest();
        validateData(request.data());
        if (request.data().primaryDeptId() == null) throw ApiException.invalidRequest();
        validateIds(request.roleIds());
        if (!request.roleIds().isEmpty()) permissions.requireSuperAdmin(actor);
        int status = SystemStatus.databaseValue(request.status());
        ensureDestination(scope, request.data().primaryDeptId());
        try {
            return transaction.execute(tx -> {
                ensureActiveDept(request.data().primaryDeptId());
                ensureActiveRoles(request.roleIds());
                rejectSuperRole(request.roleIds());
                SysUser user = entity(null, request.data());
                user.setUsername(request.username());
                user.setPasswordHash(encoder.encode(InitialPasswordPolicy.DEFAULT_PASSWORD));
                user.setStatus(status);
                user.setMustChangePassword(1);
                users.insert(user);
                long id = user.getId();
                for (long roleId : request.roleIds()) {
                    users.insertRole(id, roleId);
                }
                ensureHomePath(id, request.data().homePath());
                audit.record(actor, "system:user:create", AuditTargetType.USER, id);
                return id;
            });
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void update(long actor, long targetId, UserData data) {
        var scope = permissions.userScope(actor, "system:user:update");
        validateData(data);
        try {
            transaction.executeWithoutResult(tx -> {
                SysUser target = lockUser(targetId);
                requireVisible(scope, target);
                protectSuperUser(actor, targetId);
                if (data.primaryDeptId() == null && !hasSuperRole(targetId)) {
                    throw ApiException.invalidRequest();
                }
                ensureDestination(scope, data.primaryDeptId());
                ensureActiveDept(data.primaryDeptId());
                users.updateDetails(entity(targetId, data));
                ensureHomePath(targetId, data.homePath());
                audit.record(actor, "system:user:update", AuditTargetType.USER, targetId);
            });
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void changeStatus(long actor, long targetId, String status) {
        var scope = permissions.userScope(actor, "system:user:status");
        int newStatus = SystemStatus.databaseValue(status);
        if (newStatus == 0 && actor == targetId) throw ApiException.forbidden();
        transaction.executeWithoutResult(tx -> {
            lockSuperRole();
            SysUser target = lockUser(targetId);
            requireVisible(scope, target);
            protectSuperUser(actor, targetId);
            if (newStatus == 0 && target.getStatus() == 1 && hasSuperRole(targetId)) {
                requireAnotherActiveSuper();
            }
            users.updateStatus(targetId, newStatus);
            if (newStatus == 0) revokeSessions(targetId);
            audit.record(actor, "system:user:status", AuditTargetType.USER, targetId);
        });
    }

    @Override
    public void replaceRoles(long actor, long targetId, List<Long> roleIds) {
        permissions.requireSuperAdmin(actor);
        validateIds(roleIds);
        transaction.executeWithoutResult(tx -> {
            lockSuperRole();
            lockUser(targetId);
            ensureActiveRoles(roleIds);
            rejectSuperRole(roleIds);
            if (hasSuperRole(targetId)) throw ApiException.forbidden();
            users.deleteRoles(targetId);
            for (long roleId : roleIds) {
                users.insertRole(targetId, roleId);
            }
            SysUser profile = users.selectOne(new LambdaQueryWrapper<SysUser>()
                    .select(SysUser::getHomePath)
                    .eq(SysUser::getId, targetId));
            String homePath = profile == null ? null : profile.getHomePath();
            if (homePath != null && !homePath.isBlank()) ensureHomePath(targetId, homePath);
            audit.record(actor, "system:user:grant", AuditTargetType.USER, targetId);
        });
    }

    @Override
    public void resetPassword(long actor, long targetId, String temporaryPassword) {
        var scope = permissions.userScope(actor, "system:user:reset-password");
        validatePassword(temporaryPassword);
        transaction.executeWithoutResult(tx -> {
            SysUser target = lockUser(targetId);
            requireVisible(scope, target);
            protectSuperUser(actor, targetId);
            users.resetPassword(targetId, encoder.encode(temporaryPassword));
            revokeSessions(targetId);
            audit.record(actor, "system:user:reset-password", AuditTargetType.USER, targetId);
        });
    }

    @Override
    public void updateProfile(long actor, ProfileData data) {
        validateProfile(data);
        try {
            transaction.executeWithoutResult(tx -> {
                lockUser(actor);
                SysUser user = new SysUser();
                user.setId(actor);
                user.setRealName(data.realName());
                user.setEmail(data.email());
                user.setPhone(data.phone());
                user.setAvatarUrl(data.avatarUrl());
                users.updateProfile(user);
                audit.record(actor, "user:profile", AuditTargetType.USER, actor);
            });
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict();
        }
    }

    private SysUser lockUser(long id) {
        SysUser row = users.lock(id);
        if (row == null) throw ApiException.notFound();
        return row;
    }

    private void requireVisible(IManagementPermissionService.UserScope scope, SysUser target) {
        if (!scope.all()
                && target.getId() != scope.actorId()
                && (target.getPrimaryDeptId() == null || !scope.deptIds().contains(target.getPrimaryDeptId()))) {
            throw ApiException.forbidden();
        }
    }

    private void ensureDestination(IManagementPermissionService.UserScope scope, Long deptId) {
        if (!scope.all() && (deptId == null || !scope.deptIds().contains(deptId))) {
            throw ApiException.forbidden();
        }
    }

    private void protectSuperUser(long actor, long targetId) {
        if (hasSuperRole(targetId) && !permissions.isSuperAdmin(actor)) {
            throw ApiException.forbidden();
        }
    }

    private boolean hasSuperRole(long userId) {
        return users.countSuperRole(userId) > 0;
    }

    // Lock the built-in role before counting active admins so two concurrent removals cannot both succeed.
    private void lockSuperRole() {
        if (users.lockSuperRole() == null) throw ApiException.conflict();
    }

    private Long superRoleId() {
        return users.superRoleId();
    }

    private void rejectSuperRole(List<Long> roleIds) {
        if (!roleIds.isEmpty() && roleIds.contains(superRoleId())) {
            throw ApiException.forbidden();
        }
    }

    private void requireAnotherActiveSuper() {
        if (users.countActiveSuperUsers() <= 1) throw ApiException.forbidden();
    }

    private void ensureActiveDept(Long deptId) {
        if (deptId == null) return;
        if (users.countActiveDepartment(deptId) == 0) throw ApiException.conflict();
    }

    private void ensureActiveRoles(List<Long> ids) {
        if (ids.isEmpty()) return;
        if (users.countActiveRoles(ids) != ids.size()) throw ApiException.conflict();
    }

    private void ensureHomePath(long userId, String path) {
        String normalized = normalizedPath(path);
        if (normalized == null) return;
        if (!containsPage(access.menus(userId), normalized)) throw ApiException.invalidRequest();
    }

    private boolean containsPage(List<MenuRouteVO> routes, String path) {
        for (MenuRouteVO route : routes) {
            if (route.component() != null && path.equals(route.path())) return true;
            if (containsPage(route.children(), path)) return true;
        }
        return false;
    }

    private String normalizedPath(String path) {
        return path == null || path.isBlank() ? null : path;
    }

    private void revokeSessions(long userId) {
        sessions.revokeAllForUser(userId, DateUtils.now(clock));
    }

    private void validateIds(List<Long> ids) {
        if (ids == null
                || ids.size() > 1000
                || ids.stream().anyMatch(id -> id == null || id < 1)
                || new HashSet<>(ids).size() != ids.size()) throw ApiException.invalidRequest();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw ApiException.invalidRequest();
        }
    }

    private void validateData(UserData data) {
        if (data == null) throw ApiException.invalidRequest();
        validateProfile(new ProfileData(data.realName(), data.email(), data.phone(), data.avatarUrl()));
        if (data.primaryDeptId() != null && data.primaryDeptId() < 1
                || data.homePath() != null && data.homePath().length() > 255
                || data.remark() != null && data.remark().length() > 500) {
            throw ApiException.invalidRequest();
        }
    }

    private void validateProfile(ProfileData data) {
        if (data == null
                || data.realName() == null
                || data.realName().isBlank()
                || data.realName().length() > 128
                || data.email() != null
                        && (data.email().length() > 255
                                || !EMAIL.matcher(data.email()).matches())
                || data.phone() != null && data.phone().length() > 32
                || data.avatarUrl() != null && data.avatarUrl().length() > 1024) {
            throw ApiException.invalidRequest();
        }
    }

    private SysUser entity(Long id, UserData data) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRealName(data.realName());
        user.setRemark(data.remark());
        user.setEmail(data.email());
        user.setPhone(data.phone());
        user.setPrimaryDeptId(data.primaryDeptId());
        user.setAvatarUrl(data.avatarUrl());
        user.setHomePath(normalizedPath(data.homePath()));
        return user;
    }
}
