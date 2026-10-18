package com.maplehush.admin.modules.system.menu.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.menu.dto.CreateMenuDTO;
import com.maplehush.admin.modules.system.menu.dto.UpdateMenuDTO;
import com.maplehush.admin.modules.system.menu.service.ISysMenuQueryService;
import com.maplehush.admin.modules.system.menu.service.ISysMenuService;
import com.maplehush.admin.modules.system.menu.vo.MenuDetailVO;
import com.maplehush.admin.modules.system.menu.vo.MenuNodeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysMenuController {
    private final ISysMenuService menus;
    private final ISysMenuQueryService queries;

    public SysMenuController(ISysMenuService menus, ISysMenuQueryService queries) {
        this.menus = menus;
        this.queries = queries;
    }

    @PostMapping("/system/menus")
    public ApiResult<Boolean> create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody CreateMenuDTO request) {
        menus.create(actor.userId(), request.type(), request.menuKey(), request.authCode(), request.data());
        return ApiResult.ok(true);
    }

    @PutMapping("/system/menus/{menuId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String menuId,
            @Valid @RequestBody UpdateMenuDTO request) {
        menus.update(actor.userId(), id(menuId), request.data());
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/menus/{menuId}")
    public ApiResult<Boolean> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable String menuId) {
        menus.delete(actor.userId(), id(menuId));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/menus/tree")
    public ApiResult<List<MenuNodeVO>> tree(@AuthenticationPrincipal CurrentUser actor) {
        return ApiResult.ok(queries.tree(actor.userId()));
    }

    @GetMapping("/system/menus/{menuId}")
    public ApiResult<MenuDetailVO> detail(@AuthenticationPrincipal CurrentUser actor, @PathVariable String menuId) {
        return ApiResult.ok(queries.detail(actor.userId(), id(menuId)));
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
