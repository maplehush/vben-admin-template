package com.maplehush.admin.modules.system.role.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.role.dto.CreateRoleDTO;
import com.maplehush.admin.modules.system.role.dto.RoleDataScopeDTO;
import com.maplehush.admin.modules.system.role.dto.RoleMenusDTO;
import com.maplehush.admin.modules.system.role.dto.RolePermissionsDTO;
import com.maplehush.admin.modules.system.role.dto.RoleQueryDTO;
import com.maplehush.admin.modules.system.role.dto.UpdateRoleDTO;
import com.maplehush.admin.modules.system.role.service.ISysRoleQueryService;
import com.maplehush.admin.modules.system.role.service.ISysRoleService;
import com.maplehush.admin.modules.system.role.vo.RoleDataScopeVO;
import com.maplehush.admin.modules.system.role.vo.RoleDetailVO;
import com.maplehush.admin.modules.system.role.vo.RoleMenusVO;
import com.maplehush.admin.modules.system.role.vo.RolePermissionsVO;
import com.maplehush.admin.modules.system.role.vo.RoleSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
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
public class SysRoleController {
    private final ISysRoleService roles;
    private final ISysRoleQueryService queries;

    public SysRoleController(ISysRoleService roles, ISysRoleQueryService queries) {
        this.roles = roles;
        this.queries = queries;
    }

    @PostMapping("/system/roles")
    public ApiResult<Boolean> create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody CreateRoleDTO request) {
        roles.create(
                actor.userId(),
                request.roleCode(),
                request.roleName(),
                request.description(),
                request.status(),
                optionalIds(request.menuIds()));
        return ApiResult.ok(true);
    }

    @PutMapping("/system/roles/{roleId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String roleId,
            @Valid @RequestBody UpdateRoleDTO request) {
        roles.update(
                actor.userId(),
                id(roleId),
                request.roleName(),
                request.description(),
                request.status(),
                optionalIds(request.menuIds()));
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/roles/{roleId}")
    public ApiResult<Boolean> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable String roleId) {
        roles.delete(actor.userId(), id(roleId));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/roles/{roleId}/menus")
    public ApiResult<RoleMenusVO> menus(@AuthenticationPrincipal CurrentUser actor, @PathVariable String roleId) {
        return ApiResult.ok(new RoleMenusVO(roles.menus(actor.userId(), id(roleId))));
    }

    @PutMapping("/system/roles/{roleId}/menus")
    public ApiResult<Boolean> replaceMenus(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String roleId,
            @Valid @RequestBody RoleMenusDTO request) {
        roles.replaceMenus(actor.userId(), id(roleId), ids(request.menuIds()));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/roles/{roleId}/permissions")
    public ApiResult<RolePermissionsVO> permissions(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String roleId) {
        return ApiResult.ok(new RolePermissionsVO(roles.permissions(actor.userId(), id(roleId))));
    }

    @PutMapping("/system/roles/{roleId}/permissions")
    public ApiResult<Boolean> replacePermissions(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String roleId,
            @Valid @RequestBody RolePermissionsDTO request) {
        roles.replacePermissions(actor.userId(), id(roleId), ids(request.permissionIds()));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/roles/{roleId}/data-scope")
    public ApiResult<RoleDataScopeVO> dataScope(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String roleId) {
        var scope = roles.dataScope(actor.userId(), id(roleId));
        return ApiResult.ok(new RoleDataScopeVO(scope.type(), scope.deptIds()));
    }

    @PutMapping("/system/roles/{roleId}/data-scope")
    public ApiResult<Boolean> updateDataScope(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String roleId,
            @Valid @RequestBody RoleDataScopeDTO request) {
        roles.updateDataScope(actor.userId(), id(roleId), request.type(), ids(request.deptIds()));
        return ApiResult.ok(true);
    }

    private List<Long> ids(List<String> values) {
        if (values == null) throw ApiException.invalidRequest();
        return values.stream().map(this::id).toList();
    }

    private List<Long> optionalIds(List<String> values) {
        return values == null ? null : ids(values);
    }

    @GetMapping("/system/roles")
    public ApiResult<PageResult<RoleSummaryVO>> roles(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResult.ok(queries.roles(
                actor.userId(),
                new RoleQueryDTO(
                        page,
                        pageSize,
                        roleId == null || roleId.isBlank() ? null : id(roleId),
                        roleName,
                        status,
                        description,
                        startTime,
                        endTime)));
    }

    @GetMapping("/system/roles/{roleId}")
    public ApiResult<RoleDetailVO> role(@AuthenticationPrincipal CurrentUser actor, @PathVariable String roleId) {
        return ApiResult.ok(queries.role(actor.userId(), id(roleId)));
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
