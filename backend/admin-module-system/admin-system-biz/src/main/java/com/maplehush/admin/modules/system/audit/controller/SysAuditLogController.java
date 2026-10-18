package com.maplehush.admin.modules.system.audit.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.audit.dto.LoginLogQueryDTO;
import com.maplehush.admin.modules.system.audit.dto.OperationLogQueryDTO;
import com.maplehush.admin.modules.system.audit.service.IAuditLogQueryService;
import com.maplehush.admin.modules.system.audit.vo.LoginLogVO;
import com.maplehush.admin.modules.system.audit.vo.OperationLogVO;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysAuditLogController {
    private final IAuditLogQueryService logs;

    public SysAuditLogController(IAuditLogQueryService logs) {
        this.logs = logs;
    }

    @GetMapping("/system/login-logs")
    public ApiResult<PageResult<LoginLogVO>> loginLogs(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResult.ok(logs.loginLogs(
                actor.userId(), new LoginLogQueryDTO(page, pageSize, username, success, startTime, endTime)));
    }

    @GetMapping("/system/operation-logs")
    public ApiResult<PageResult<OperationLogVO>> operationLogs(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResult.ok(logs.operationLogs(
                actor.userId(), new OperationLogQueryDTO(page, pageSize, username, actionCode, startTime, endTime)));
    }
}
