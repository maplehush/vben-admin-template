package com.maplehush.admin.modules.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.role.dto.RoleQueryDTO;
import com.maplehush.admin.modules.system.role.entity.SysRole;
import com.maplehush.admin.modules.system.role.mapper.SysRoleMapper;
import com.maplehush.admin.modules.system.role.service.ISysRoleQueryService;
import com.maplehush.admin.modules.system.role.vo.RoleDetailVO;
import com.maplehush.admin.modules.system.role.vo.RoleSummaryVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** 查询角色列表和详情，并校验对应读取权限。 */
@Service
public class SysRoleQueryServiceImpl implements ISysRoleQueryService {
    private final SysRoleMapper roles;
    private final IManagementPermissionService permissions;

    public SysRoleQueryServiceImpl(SysRoleMapper roles, IManagementPermissionService permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public PageResult<RoleSummaryVO> roles(long actor, RoleQueryDTO query) {
        permissions.require(actor, "system:role:list");
        checkPage(query.page(), query.pageSize());
        var conditions = new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getId);
        if (query.roleId() != null) conditions.eq(SysRole::getId, query.roleId());
        String name = like(query.roleName());
        String description = like(query.description());
        if (name != null) conditions.like(SysRole::getRoleName, name);
        if (description != null) conditions.like(SysRole::getDescription, description);
        if (query.status() != null) conditions.eq(SysRole::getStatus, SystemStatus.databaseValue(query.status()));
        LocalDateTime start = time(query.startTime());
        LocalDateTime end = time(query.endTime());
        if (start != null) conditions.ge(SysRole::getCreateTime, start);
        if (end != null) conditions.lt(SysRole::getCreateTime, end);
        var page = roles.selectPage(new Page<>(query.page(), query.pageSize()), conditions);
        return new PageResult<>(
                page.getRecords().stream().map(this::roleSummary).toList(), page.getTotal());
    }

    @Override
    public RoleDetailVO role(long actor, long id) {
        permissions.require(actor, "system:role:view");
        SysRole row = roles.selectById(id);
        if (row == null) throw ApiException.notFound();
        return new RoleDetailVO(roleSummary(row), row.getUpdateTime());
    }

    private String like(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 128) throw ApiException.invalidRequest();
        return value.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void checkPage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw ApiException.invalidRequest();
    }

    private LocalDateTime time(String value) {
        if (value == null) return null;
        try {
            return DateUtils.fromIso(value);
        } catch (RuntimeException exception) {
            throw ApiException.invalidRequest();
        }
    }

    private RoleSummaryVO roleSummary(SysRole row) {
        return new RoleSummaryVO(
                Long.toString(row.getId()),
                row.getRoleCode(),
                row.getRoleName(),
                row.getDescription(),
                SystemStatus.apiValue(row.getStatus()),
                row.getDataScopeType(),
                row.getCreateTime());
    }
}
