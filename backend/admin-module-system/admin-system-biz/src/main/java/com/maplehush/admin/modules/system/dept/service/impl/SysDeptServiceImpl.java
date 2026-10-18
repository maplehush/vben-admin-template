package com.maplehush.admin.modules.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.entity.SysDept;
import com.maplehush.admin.modules.system.dept.mapper.SysDeptMapper;
import com.maplehush.admin.modules.system.dept.service.ISysDeptService;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 维护部门树、编号、状态和删除约束。 */
@Service
public class SysDeptServiceImpl implements ISysDeptService {
    private final SysDeptMapper departments;
    private final IManagementPermissionService permissions;
    private final IOperationAuditService audit;
    private final TransactionTemplate transaction;
    private final String codePrefix;

    public SysDeptServiceImpl(
            SysDeptMapper departments,
            IManagementPermissionService permissions,
            IOperationAuditService audit,
            PlatformTransactionManager transactionManager,
            @Value("${app.dept.code-prefix:DEPT_}") String codePrefix) {
        if (codePrefix == null || !codePrefix.matches("[A-Z]{2,8}_?")) {
            throw new IllegalArgumentException(
                    "app.dept.code-prefix must contain 2-8 uppercase letters and an optional trailing underscore");
        }
        this.departments = departments;
        this.permissions = permissions;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
        this.codePrefix = codePrefix;
    }

    @Override
    public long create(long actor, Long parentId, String name, int sortOrder, String status, String remark) {
        permissions.require(actor, "system:dept:create");
        validateName(name);
        String normalizedRemark = normalizedRemark(remark);
        // The temporary unique value satisfies the NOT NULL constraint until the generated ID is known.
        String temporaryCode = "pending_" + UUID.randomUUID().toString().replace("-", "");
        int statusValue = SystemStatus.databaseValue(status);
        try {
            return transaction.execute(tx -> {
                ensureParentExists(parentId);
                SysDept department = new SysDept();
                department.setParentId(parentId);
                department.setDeptCode(temporaryCode);
                department.setDeptName(name.trim());
                department.setSortOrder(sortOrder);
                department.setStatus(statusValue);
                department.setRemark(normalizedRemark);
                departments.insert(department);
                long id = department.getId();
                departments.setCode(id, codePrefix + String.format(Locale.ROOT, "%04d", id));
                audit.record(actor, "system:dept:create", AuditTargetType.DEPT, id);
                return id;
            });
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict();
        }
    }

    @Override
    public void update(long actor, long id, Long parentId, String name, int sortOrder, String status, String remark) {
        permissions.require(actor, "system:dept:update");
        validateName(name);
        String normalizedRemark = normalizedRemark(remark);
        int statusValue = SystemStatus.databaseValue(status);
        transaction.executeWithoutResult(tx -> {
            SysDept current = lock(id);
            if (parentId != null && parentId == id) throw ApiException.conflict();
            // Moving a node under its own descendant would corrupt the entire department tree.
            if (parentId != null && !parentId.equals(current.getParentId())) {
                ensureParentExists(parentId);
                ensureNotDescendant(id, parentId);
            }
            if (statusValue == 0 && current.getStatus() == 1) {
                if (departments.countActiveUsers(id) > 0) {
                    throw ApiException.conflict();
                }
            }
            departments.updateDetails(id, parentId, name.trim(), sortOrder, statusValue, normalizedRemark);
            audit.record(actor, "system:dept:update", AuditTargetType.DEPT, id);
        });
    }

    @Override
    public void delete(long actor, long id) {
        permissions.require(actor, "system:dept:delete");
        transaction.executeWithoutResult(tx -> {
            SysDept current = lock(id);
            if (current.getStatus() == 1) throw ApiException.conflict();
            if (departments.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0
                    || departments.countUsers(id) > 0
                    || departments.countScopeReferences(id) > 0) {
                throw ApiException.conflict();
            }
            departments.deleteById(id);
            audit.record(actor, "system:dept:delete", AuditTargetType.DEPT, id);
        });
    }

    private SysDept lock(long id) {
        SysDept row = departments.lock(id);
        if (row == null) throw ApiException.notFound();
        return row;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == null) return;
        if (departments.lockParent(parentId) == null) throw ApiException.conflict();
    }

    private void ensureNotDescendant(long movingId, long parentId) {
        Set<Long> seen = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null) {
            if (cursor == movingId || !seen.add(cursor)) throw ApiException.conflict();
            cursor = departments.parentOf(cursor);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 128) throw ApiException.invalidRequest();
    }

    private String normalizedRemark(String remark) {
        if (remark == null || remark.isBlank()) return null;
        String value = remark.trim();
        if (value.length() > 500) throw ApiException.invalidRequest();
        return value;
    }
}
