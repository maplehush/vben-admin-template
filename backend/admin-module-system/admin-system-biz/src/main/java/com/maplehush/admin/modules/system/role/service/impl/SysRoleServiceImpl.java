package com.maplehush.admin.modules.system.role.service.impl;

import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.constant.SystemConstants;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.role.entity.SysRole;
import com.maplehush.admin.modules.system.role.mapper.SysRoleMapper;
import com.maplehush.admin.modules.system.role.service.ISysRoleService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 管理角色及其菜单、接口权限和数据范围授权。 */
@Service
public class SysRoleServiceImpl implements ISysRoleService {
    private static final Pattern ROLE_CODE = Pattern.compile("[A-Za-z0-9_.-]{3,64}");
    private static final Set<String> SCOPE_TYPES = Set.of("ALL", "DEPT_AND_CHILDREN", "DEPT", "SELF", "CUSTOM");
    private final SysRoleMapper roles;
    private final IManagementPermissionService permissions;
    private final IOperationAuditService audit;
    private final TransactionTemplate transaction;

    public SysRoleServiceImpl(
            SysRoleMapper roles,
            IManagementPermissionService permissions,
            IOperationAuditService audit,
            PlatformTransactionManager transactionManager) {
        this.roles = roles;
        this.permissions = permissions;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public long create(long actor, String code, String name, String description, String status) {
        return create(actor, code, name, description, status, null);
    }

    public long create(long actor, String code, String name, String description, String status, List<Long> menuIds) {
        permissions.requireSuperAdmin(actor);
        if (code == null || !ROLE_CODE.matcher(code).matches()) throw ApiException.invalidRequest();
        validateFields(name, description, status);
        if (menuIds != null) validateIds(menuIds);
        try {
            return transaction.execute(tx -> {
                if (menuIds != null) ensureActiveIds(ResourceKind.MENU, menuIds);
                SysRole role = new SysRole();
                role.setRoleCode(code);
                role.setRoleName(name);
                role.setDescription(description);
                role.setStatus(SystemStatus.databaseValue(status));
                role.setDataScopeType("SELF");
                roles.insert(role);
                long id = role.getId();
                if (menuIds != null) insertMenus(id, menuIds);
                audit.record(actor, "system:role:create", AuditTargetType.ROLE, id);
                return id;
            });
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void update(long actor, long id, String name, String description, String status) {
        update(actor, id, name, description, status, null);
    }

    public void update(long actor, long id, String name, String description, String status, List<Long> menuIds) {
        permissions.requireSuperAdmin(actor);
        validateFields(name, description, status);
        if (menuIds != null) validateIds(menuIds);
        transaction.executeWithoutResult(tx -> {
            lockWritable(id);
            if (menuIds != null) ensureActiveIds(ResourceKind.MENU, menuIds);
            roles.updateDetails(id, name, description, SystemStatus.databaseValue(status));
            if (menuIds != null) {
                roles.deleteMenus(id);
                insertMenus(id, menuIds);
            }
            audit.record(actor, "system:role:update", AuditTargetType.ROLE, id);
        });
    }

    @Override
    public void delete(long actor, long id) {
        permissions.requireSuperAdmin(actor);
        transaction.executeWithoutResult(tx -> {
            lockWritable(id);
            if (roles.countAssignedUsers(id) > 0) throw ApiException.conflict();
            roles.deleteMenus(id);
            roles.deletePermissions(id);
            roles.deleteDepartments(id);
            roles.deleteById(id);
            audit.record(actor, "system:role:delete", AuditTargetType.ROLE, id);
        });
    }

    @Override
    public List<String> menus(long actor, long id) {
        permissions.require(actor, "system:role:grant");
        ensureExists(id);
        return roles.menuIds(id).stream().map(String::valueOf).toList();
    }

    @Override
    public void replaceMenus(long actor, long id, List<Long> ids) {
        permissions.requireSuperAdmin(actor);
        validateIds(ids);
        transaction.executeWithoutResult(tx -> {
            lockWritable(id);
            ensureActiveIds(ResourceKind.MENU, ids);
            roles.deleteMenus(id);
            insertMenus(id, ids);
            audit.record(actor, "system:role:grant", AuditTargetType.ROLE, id);
        });
    }

    private void insertMenus(long roleId, List<Long> ids) {
        for (long menuId : ids) {
            roles.insertMenu(roleId, menuId);
        }
    }

    @Override
    public List<String> permissions(long actor, long id) {
        this.permissions.require(actor, "system:role:grant");
        ensureExists(id);
        return roles.permissionIds(id).stream().map(String::valueOf).toList();
    }

    @Override
    public void replacePermissions(long actor, long id, List<Long> ids) {
        permissions.requireSuperAdmin(actor);
        validateIds(ids);
        transaction.executeWithoutResult(tx -> {
            lockWritable(id);
            ensureActiveIds(ResourceKind.PERMISSION, ids);
            roles.deletePermissions(id);
            for (long permissionId : ids) {
                roles.insertPermission(id, permissionId);
            }
            audit.record(actor, "system:role:grant", AuditTargetType.ROLE, id);
        });
    }

    @Override
    public DataScope dataScope(long actor, long id) {
        permissions.require(actor, "system:role:data-scope");
        String type = roles.dataScopeType(id);
        if (type == null) throw ApiException.notFound();
        List<String> deptIds =
                roles.departmentIds(id).stream().map(String::valueOf).toList();
        return new DataScope(type, deptIds);
    }

    @Override
    public void updateDataScope(long actor, long id, String type, List<Long> deptIds) {
        permissions.requireSuperAdmin(actor);
        if (type == null || !SCOPE_TYPES.contains(type)) throw ApiException.invalidRequest();
        validateIds(deptIds);
        if (!"CUSTOM".equals(type) && !deptIds.isEmpty()) throw ApiException.invalidRequest();
        transaction.executeWithoutResult(tx -> {
            lockWritable(id);
            if ("CUSTOM".equals(type)) ensureActiveIds(ResourceKind.DEPARTMENT, deptIds);
            roles.updateDataScope(id, type);
            roles.deleteDepartments(id);
            for (long deptId : deptIds) {
                roles.insertDepartment(id, deptId);
            }
            audit.record(actor, "system:role:data-scope", AuditTargetType.ROLE, id);
        });
    }

    // Lock the owning role before replacing grants, so concurrent replacements serialize.
    private void lockWritable(long id) {
        String code = roles.lockRoleCode(id);
        if (code == null) throw ApiException.notFound();
        if (SystemConstants.SUPER_ADMIN_ROLE_CODE.equals(code)) throw ApiException.forbidden();
    }

    private void ensureExists(long id) {
        if (roles.countRole(id) == 0) throw ApiException.notFound();
    }

    private void ensureActiveIds(ResourceKind kind, List<Long> ids) {
        if (ids.isEmpty()) return;
        long count =
                switch (kind) {
                    case MENU -> roles.countActiveMenus(ids);
                    case PERMISSION -> roles.countActivePermissions(ids);
                    case DEPARTMENT -> roles.countActiveDepartments(ids);
                };
        if (count != ids.size()) throw ApiException.conflict();
    }

    private void validateFields(String name, String description, String status) {
        if (name == null
                || name.isBlank()
                || name.length() > 128
                || (description != null && description.length() > 500)) {
            throw ApiException.invalidRequest();
        }
        SystemStatus.databaseValue(status);
    }

    private void validateIds(List<Long> ids) {
        if (ids == null
                || ids.size() > 1000
                || ids.stream().anyMatch(id -> id == null || id < 1)
                || new HashSet<>(ids).size() != ids.size()) {
            throw ApiException.invalidRequest();
        }
    }

    private enum ResourceKind {
        MENU,
        PERMISSION,
        DEPARTMENT
    }
}
