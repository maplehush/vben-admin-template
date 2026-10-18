package com.maplehush.admin.modules.system.user.service.impl;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import com.maplehush.admin.modules.system.user.dto.UserQueryDTO;
import com.maplehush.admin.modules.system.user.mapper.SysUserQueryFilter;
import com.maplehush.admin.modules.system.user.mapper.SysUserQueryMapper;
import com.maplehush.admin.modules.system.user.service.ISysUserQueryService;
import com.maplehush.admin.modules.system.user.vo.UserDetailVO;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** 按操作权限对应的数据范围查询用户列表和详情。 */
@Service
public class SysUserQueryServiceImpl implements ISysUserQueryService {
    private final SysUserQueryMapper queries;
    private final IManagementPermissionService permissions;

    public SysUserQueryServiceImpl(SysUserQueryMapper queries, IManagementPermissionService permissions) {
        this.queries = queries;
        this.permissions = permissions;
    }

    @Override
    public PageResult<UserSummaryVO> users(long actor, UserQueryDTO query) {
        checkPage(query.page(), query.pageSize());
        var scope = permissions.userScope(actor, "system:user:list");
        var filter = new SysUserQueryFilter(
                scope.all(),
                scope.actorId(),
                scope.deptIds(),
                like(query.username()),
                like(query.realName()),
                like(query.remark()),
                query.deptId(),
                query.userId(),
                query.status() == null ? null : SystemStatus.databaseValue(query.status()),
                time(query.startTime()),
                time(query.endTime()));
        long total = queries.countUsers(filter);
        List<UserSummaryVO> items =
                queries.listUsers(filter, query.pageSize(), ((long) query.page() - 1) * query.pageSize());
        return new PageResult<>(items, total);
    }

    @Override
    public UserDetailVO user(long actor, long id) {
        var scope = permissions.userScope(actor, "system:user:view");
        var filter = new SysUserQueryFilter(
                scope.all(), scope.actorId(), scope.deptIds(), null, null, null, null, id, null, null, null);
        UserDetailVO row = queries.findUser(filter);
        if (row != null) {
            return row.withRoleIds(
                    queries.userRoleIds(id).stream().map(String::valueOf).toList());
        }
        if (queries.countUser(id) > 0) throw ApiException.forbidden();
        throw ApiException.notFound();
    }

    private String like(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 128) throw ApiException.invalidRequest();
        return "%" + value.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
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
}
