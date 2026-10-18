package com.maplehush.admin.modules.system.permission.service.impl;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.dto.PermissionQueryDTO;
import com.maplehush.admin.modules.system.permission.entity.SysPermission;
import com.maplehush.admin.modules.system.permission.mapper.SysPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.permission.service.ISysPermissionService;
import com.maplehush.admin.modules.system.permission.vo.PermissionVO;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 维护独立接口权限资源及其状态。 */
@Service
public class SysPermissionServiceImpl implements ISysPermissionService {
    private static final Pattern KEY = Pattern.compile("[A-Za-z0-9_.-]{3,128}");
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9:_-]{3,128}");
    private final SysPermissionMapper mapper;
    private final IManagementPermissionService permissions;
    private final IOperationAuditService audit;
    private final TransactionTemplate transaction;

    public SysPermissionServiceImpl(
            SysPermissionMapper mapper,
            IManagementPermissionService permissions,
            IOperationAuditService audit,
            PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.permissions = permissions;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public PageResult<PermissionVO> list(long actor, PermissionQueryDTO query) {
        permissions.require(actor, "system:permission:list");
        int page = query.page();
        int pageSize = query.pageSize();
        String code = query.code();
        if (page < 1
                || pageSize < 1
                || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE
                || code != null && code.length() > 128) throw ApiException.invalidRequest();
        String filter = code == null || code.isBlank() ? null : code.trim();
        String pattern = filter == null
                ? null
                : "%" + filter.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        long total = mapper.countFiltered(pattern);
        List<PermissionVO> items = mapper.listFiltered(pattern, pageSize, (page - 1) * pageSize).stream()
                .map(this::view)
                .toList();
        return new PageResult<>(items, total);
    }

    @Override
    public PermissionVO detail(long actor, long id) {
        permissions.require(actor, "system:permission:view");
        return load(id);
    }

    @Override
    public long create(long actor, String key, String code, PermissionData data) {
        permissions.requireSuperAdmin(actor);
        if (key == null
                || !KEY.matcher(key).matches()
                || code == null
                || !CODE.matcher(code).matches()) throw ApiException.invalidRequest();
        validate(data);
        try {
            return transaction.execute(tx -> {
                ensureMenu(data.menuId());
                lockPermissionNamespace();
                ensureCodeNotOnMenu(code);
                SysPermission permission = new SysPermission();
                permission.setMenuId(data.menuId());
                permission.setPermissionKey(key);
                permission.setPermissionCode(code);
                permission.setTitleKey(data.titleKey().trim());
                permission.setSortOrder(data.sortOrder());
                permission.setStatus(SystemStatus.databaseValue(data.status()));
                mapper.insert(permission);
                long id = permission.getId();
                audit.record(actor, "system:permission:create", AuditTargetType.PERMISSION, id);
                return id;
            });
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void update(long actor, long id, PermissionData data) {
        permissions.requireSuperAdmin(actor);
        validate(data);
        try {
            transaction.executeWithoutResult(tx -> {
                PermissionVO current = load(id);
                if (protectedKey(current.permissionKey())) throw ApiException.forbidden();
                ensureMenu(data.menuId());
                mapper.updateDetails(
                        id,
                        data.menuId(),
                        data.titleKey().trim(),
                        data.sortOrder(),
                        SystemStatus.databaseValue(data.status()));
                audit.record(actor, "system:permission:update", AuditTargetType.PERMISSION, id);
            });
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void delete(long actor, long id) {
        permissions.requireSuperAdmin(actor);
        transaction.executeWithoutResult(tx -> {
            PermissionVO current = load(id);
            if (protectedKey(current.permissionKey())) throw ApiException.forbidden();
            if (mapper.countRoleGrants(id) > 0) throw ApiException.conflict();
            mapper.deleteById(id);
            audit.record(actor, "system:permission:delete", AuditTargetType.PERMISSION, id);
        });
    }

    private boolean protectedKey(String key) {
        return key.startsWith("perm.system.");
    }

    // Menu button writes take the same lock before checking cross-table code uniqueness.
    private void lockPermissionNamespace() {
        mapper.lockPermissionNamespace();
    }

    private PermissionVO load(long id) {
        SysPermission row = mapper.selectById(id);
        if (row == null) throw ApiException.notFound();
        return view(row);
    }

    private PermissionVO view(SysPermission row) {
        Long menuId = row.getMenuId();
        return new PermissionVO(
                Long.toString(row.getId()),
                menuId == null ? null : menuId.toString(),
                row.getPermissionKey(),
                row.getPermissionCode(),
                row.getTitleKey(),
                row.getSortOrder(),
                SystemStatus.apiValue(row.getStatus()),
                row.getCreateTime(),
                row.getUpdateTime());
    }

    private void validate(PermissionData data) {
        if (data == null
                || data.titleKey() == null
                || data.titleKey().isBlank()
                || data.titleKey().length() > 255) throw ApiException.invalidRequest();
        SystemStatus.databaseValue(data.status());
    }

    private void ensureMenu(Long id) {
        if (id == null) return;
        if (mapper.countEligibleMenu(id) == 0) throw ApiException.conflict();
    }

    private void ensureCodeNotOnMenu(String code) {
        if (mapper.countMenuCode(code) > 0) throw ApiException.conflict();
    }
}
