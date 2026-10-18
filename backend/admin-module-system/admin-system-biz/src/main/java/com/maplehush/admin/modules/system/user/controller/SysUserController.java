package com.maplehush.admin.modules.system.user.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.user.dto.CreateUserDTO;
import com.maplehush.admin.modules.system.user.dto.ReplaceUserRolesDTO;
import com.maplehush.admin.modules.system.user.dto.ResetPasswordDTO;
import com.maplehush.admin.modules.system.user.dto.UpdateProfileDTO;
import com.maplehush.admin.modules.system.user.dto.UpdateStatusDTO;
import com.maplehush.admin.modules.system.user.dto.UpdateUserDTO;
import com.maplehush.admin.modules.system.user.dto.UserQueryDTO;
import com.maplehush.admin.modules.system.user.service.ISysUserQueryService;
import com.maplehush.admin.modules.system.user.service.ISysUserService;
import com.maplehush.admin.modules.system.user.service.ISysUserService.CreateUser;
import com.maplehush.admin.modules.system.user.service.ISysUserService.ProfileData;
import com.maplehush.admin.modules.system.user.vo.UserDetailVO;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysUserController {
    private final ISysUserService users;
    private final ISysUserQueryService queries;

    public SysUserController(ISysUserService users, ISysUserQueryService queries) {
        this.users = users;
        this.queries = queries;
    }

    @PostMapping("/system/users")
    public ApiResult<Boolean> create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody CreateUserDTO request) {
        users.create(
                actor.userId(),
                new CreateUser(request.username(), request.data(), request.status(), ids(request.roleIds())));
        return ApiResult.ok(true);
    }

    @PutMapping("/system/users/{userId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserDTO request) {
        users.update(actor.userId(), id(userId), request.data());
        return ApiResult.ok(true);
    }

    @PutMapping("/system/users/{userId}/status")
    public ApiResult<Boolean> changeStatus(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String userId,
            @Valid @RequestBody UpdateStatusDTO request) {
        users.changeStatus(actor.userId(), id(userId), request.status());
        return ApiResult.ok(true);
    }

    @PutMapping("/system/users/{userId}/roles")
    public ApiResult<Boolean> replaceRoles(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String userId,
            @Valid @RequestBody ReplaceUserRolesDTO request) {
        users.replaceRoles(actor.userId(), id(userId), ids(request.roleIds()));
        return ApiResult.ok(true);
    }

    @PostMapping("/system/users/{userId}/reset-password")
    public ApiResult<Boolean> resetPassword(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String userId,
            @Valid @RequestBody ResetPasswordDTO request) {
        users.resetPassword(actor.userId(), id(userId), request.temporaryPassword());
        return ApiResult.ok(true);
    }

    @PutMapping("/user/profile")
    public ApiResult<Boolean> profile(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody UpdateProfileDTO request) {
        users.updateProfile(
                actor.userId(),
                new ProfileData(request.realName(), request.email(), request.phone(), request.avatarUrl()));
        return ApiResult.ok(true);
    }

    private List<Long> ids(List<String> values) {
        if (values == null) throw ApiException.invalidRequest();
        return values.stream().map(this::id).toList();
    }

    @GetMapping("/system/users")
    public ApiResult<PageResult<UserSummaryVO>> users(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String deptId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResult.ok(queries.users(
                actor.userId(),
                new UserQueryDTO(
                        page,
                        pageSize,
                        username,
                        realName,
                        deptId == null ? null : id(deptId),
                        status,
                        userId == null || userId.isBlank() ? null : id(userId),
                        remark,
                        startTime,
                        endTime)));
    }

    @GetMapping("/system/users/{userId}")
    public ApiResult<UserDetailVO> user(@AuthenticationPrincipal CurrentUser actor, @PathVariable String userId) {
        return ApiResult.ok(queries.user(actor.userId(), id(userId)));
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
