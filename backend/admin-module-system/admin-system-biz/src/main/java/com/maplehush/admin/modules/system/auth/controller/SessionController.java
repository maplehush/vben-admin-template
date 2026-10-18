package com.maplehush.admin.modules.system.auth.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.dto.SessionQueryDTO;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.auth.service.ISessionManagementService;
import com.maplehush.admin.modules.system.auth.vo.SessionVO;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
    private final ISessionManagementService sessions;

    public SessionController(ISessionManagementService sessions) {
        this.sessions = sessions;
    }

    @GetMapping("/auth/sessions")
    public ApiResult<List<SessionVO>> own(@AuthenticationPrincipal CurrentUser user) {
        return ApiResult.ok(sessions.own(user.userId(), user.sessionId()));
    }

    @DeleteMapping("/auth/sessions/{sessionId}")
    public ApiResult<Boolean> revokeOwn(@AuthenticationPrincipal CurrentUser user, @PathVariable String sessionId) {
        sessions.revokeOwn(user.userId(), id(sessionId));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/sessions")
    public ApiResult<PageResult<SessionVO>> all(
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username) {
        return ApiResult.ok(sessions.all(user.userId(), new SessionQueryDTO(page, pageSize, username)));
    }

    @DeleteMapping("/system/sessions/{sessionId}")
    public ApiResult<Boolean> revokeManaged(@AuthenticationPrincipal CurrentUser user, @PathVariable String sessionId) {
        sessions.revokeManaged(user.userId(), id(sessionId));
        return ApiResult.ok(true);
    }

    private long id(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) throw ApiException.invalidRequest();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw ApiException.invalidRequest();
        }
    }
}
