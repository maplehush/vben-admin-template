package com.maplehush.admin.modules.system.permission.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.dto.CreatePermissionDTO;
import com.maplehush.admin.modules.system.permission.dto.PermissionQueryDTO;
import com.maplehush.admin.modules.system.permission.dto.UpdatePermissionDTO;
import com.maplehush.admin.modules.system.permission.service.ISysPermissionService;
import com.maplehush.admin.modules.system.permission.vo.PermissionVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysPermissionController {
    private final ISysPermissionService permissions;

    public SysPermissionController(ISysPermissionService permissions) {
        this.permissions = permissions;
    }

    @GetMapping("/system/permissions")
    public ApiResult<PageResult<PermissionVO>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String permissionCode) {
        return ApiResult.ok(permissions.list(actor.userId(), new PermissionQueryDTO(page, pageSize, permissionCode)));
    }

    @GetMapping("/system/permissions/{permissionId}")
    public ApiResult<PermissionVO> detail(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String permissionId) {
        return ApiResult.ok(permissions.detail(actor.userId(), id(permissionId)));
    }

    @PostMapping("/system/permissions")
    public ApiResult<Boolean> create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody CreatePermissionDTO request) {
        permissions.create(actor.userId(), request.permissionKey(), request.permissionCode(), request.data());
        return ApiResult.ok(true);
    }

    @PutMapping("/system/permissions/{permissionId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String permissionId,
            @Valid @RequestBody UpdatePermissionDTO request) {
        permissions.update(actor.userId(), id(permissionId), request.data());
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/permissions/{permissionId}")
    public ApiResult<Boolean> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable String permissionId) {
        permissions.delete(actor.userId(), id(permissionId));
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
