package com.maplehush.admin.modules.system.permission.service.impl;

import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.entity.SysDept;
import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import com.maplehush.admin.modules.system.permission.mapper.ManagementPermissionMapper;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.role.entity.SysRole;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 判定管理操作权限并计算该操作实际可见的数据范围。 */
@Service
public class ManagementPermissionServiceImpl implements IManagementPermissionService {
    private final ManagementPermissionMapper mapper;

    public ManagementPermissionServiceImpl(ManagementPermissionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void require(long actorId, String code) {
        if (!superAdmin(actorId) && grantingRoles(actorId, code).isEmpty()) {
            throw ApiException.forbidden();
        }
    }

    @Override
    public void requireSuperAdmin(long actorId) {
        if (!superAdmin(actorId)) throw ApiException.forbidden();
    }

    @Override
    public boolean isSuperAdmin(long actorId) {
        return superAdmin(actorId);
    }

    @Override
    public UserScope userScope(long actorId, String code) {
        if (superAdmin(actorId)) {
            return new UserScope(true, actorId, Set.of());
        }
        List<SysRole> roles = grantingRoles(actorId, code);
        if (roles.isEmpty()) {
            throw ApiException.forbidden();
        }
        Long ownDept = mapper.primaryDepartment(actorId);
        Set<Long> deptIds = new HashSet<>();
        for (SysRole role : roles) {
            switch (role.getDataScopeType()) {
                case "ALL" -> {
                    return new UserScope(true, actorId, Set.of());
                }
                case "DEPT" -> {
                    if (ownDept != null) deptIds.add(ownDept);
                }
                case "DEPT_AND_CHILDREN" -> {
                    if (ownDept != null) deptIds.addAll(descendants(ownDept));
                }
                case "CUSTOM" -> deptIds.addAll(mapper.customDepartmentIds(role.getId()));
                case "SELF" -> {}
                default -> throw ApiException.forbidden();
            }
        }
        return new UserScope(false, actorId, Set.copyOf(deptIds));
    }

    private boolean superAdmin(long actorId) {
        return mapper.countSuperAdminRoles(actorId) > 0;
    }

    private List<SysRole> grantingRoles(long actorId, String code) {
        if (!menuCodeActive(code)) return List.of();
        return mapper.grantingRoles(actorId, code);
    }

    private boolean menuCodeActive(String code) {
        SysMenu cursor = mapper.menuByCode(code);
        if (cursor == null) return true;
        Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (cursor.getStatus() != 1 || !visited.add(cursor.getId())) return false;
            if (cursor.getParentId() == null) return true;
            cursor = mapper.menuById(cursor.getParentId());
            if (cursor == null) return false;
        }
        return false;
    }

    private Set<Long> descendants(long root) {
        Map<Long, Set<Long>> children = new HashMap<>();
        for (SysDept row : mapper.departmentEdges()) {
            if (row.getParentId() != null) {
                children.computeIfAbsent(row.getParentId(), ignored -> new HashSet<>())
                        .add(row.getId());
            }
        }
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            long id = pending.removeFirst();
            if (result.add(id)) pending.addAll(children.getOrDefault(id, Set.of()));
        }
        return result;
    }
}
