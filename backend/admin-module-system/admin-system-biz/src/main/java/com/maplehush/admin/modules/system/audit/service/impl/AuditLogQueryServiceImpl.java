package com.maplehush.admin.modules.system.audit.service.impl;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.dto.LoginLogQueryDTO;
import com.maplehush.admin.modules.system.audit.dto.OperationLogQueryDTO;
import com.maplehush.admin.modules.system.audit.entity.SysLoginLog;
import com.maplehush.admin.modules.system.audit.mapper.AuditFilter;
import com.maplehush.admin.modules.system.audit.mapper.AuditLogMapper;
import com.maplehush.admin.modules.system.audit.service.IAuditLogQueryService;
import com.maplehush.admin.modules.system.audit.vo.LoginLogVO;
import com.maplehush.admin.modules.system.audit.vo.OperationLogVO;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

/** 按授权数据范围查询登录日志与操作日志。 */
@Service
public class AuditLogQueryServiceImpl implements IAuditLogQueryService {
    private final AuditLogMapper logs;
    private final IManagementPermissionService permissions;

    public AuditLogQueryServiceImpl(AuditLogMapper logs, IManagementPermissionService permissions) {
        this.logs = logs;
        this.permissions = permissions;
    }

    @Override
    public PageResult<LoginLogVO> loginLogs(long actorId, LoginLogQueryDTO query) {
        checkPage(query.page(), query.pageSize(), query.username());
        var scope = permissions.userScope(actorId, "system:login-log:list");
        var filter = filter(scope, query.username(), query.success(), null, query.startTime(), query.endTime());
        long total = logs.countLogin(filter);
        List<LoginLogVO> items =
                logs.listLogin(filter, query.pageSize(), (query.page() - 1) * query.pageSize()).stream()
                        .map(this::loginLog)
                        .toList();
        return new PageResult<>(items, total);
    }

    @Override
    public PageResult<OperationLogVO> operationLogs(long actorId, OperationLogQueryDTO query) {
        checkPage(query.page(), query.pageSize(), query.username());
        String actionCode = query.actionCode();
        if (actionCode != null && actionCode.length() > 128) throw ApiException.invalidRequest();
        var scope = permissions.userScope(actorId, "system:operation-log:list");
        var filter = filter(
                scope,
                query.username(),
                null,
                actionCode == null || actionCode.isBlank() ? null : actionCode.trim(),
                query.startTime(),
                query.endTime());
        long total = logs.countOperation(filter);
        List<OperationLogVO> items =
                logs.listOperation(filter, query.pageSize(), (query.page() - 1) * query.pageSize());
        return new PageResult<>(items, total);
    }

    private AuditFilter filter(
            IManagementPermissionService.UserScope scope,
            String username,
            Boolean success,
            String actionCode,
            String startTime,
            String endTime) {
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        if (start != null && end != null && start.isAfter(end)) throw ApiException.invalidRequest();
        return new AuditFilter(
                scope.all(),
                scope.actorId(),
                scope.deptIds(),
                username == null || username.isBlank() ? null : username.trim(),
                start,
                end,
                success,
                actionCode);
    }

    private void checkPage(int page, int pageSize, String username) {
        if (page < 1
                || pageSize < 1
                || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE
                || username != null && username.length() > 64) throw ApiException.invalidRequest();
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DateUtils.fromIso(value);
        } catch (DateTimeParseException exception) {
            throw ApiException.invalidRequest();
        }
    }

    private LoginLogVO loginLog(SysLoginLog row) {
        return new LoginLogVO(
                Long.toString(row.getId()),
                row.getUsername(),
                row.getSuccess(),
                row.getFailureReason(),
                row.getIpAddress(),
                row.getUserAgent(),
                row.getOccurTime());
    }
}
