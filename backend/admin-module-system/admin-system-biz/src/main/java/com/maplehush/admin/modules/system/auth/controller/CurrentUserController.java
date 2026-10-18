package com.maplehush.admin.modules.system.auth.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.dto.ChangePasswordDTO;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.auth.service.IOwnPasswordService;
import com.maplehush.admin.modules.system.auth.service.IUserAccessService;
import com.maplehush.admin.modules.system.auth.vo.MenuRouteVO;
import com.maplehush.admin.modules.system.auth.vo.UserInfoVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

    private final IUserAccessService access;
    private final IOwnPasswordService passwords;

    public CurrentUserController(IUserAccessService access, IOwnPasswordService passwords) {
        this.access = access;
        this.passwords = passwords;
    }

    @GetMapping("/user/info")
    public ApiResult<UserInfoVO> info(@AuthenticationPrincipal CurrentUser user) {
        return ApiResult.ok(access.info(user.userId()));
    }

    @GetMapping("/auth/codes")
    public ApiResult<List<String>> codes(@AuthenticationPrincipal CurrentUser user) {
        return ApiResult.ok(access.codes(user.userId()));
    }

    @GetMapping("/menu/all")
    public ApiResult<List<MenuRouteVO>> menus(@AuthenticationPrincipal CurrentUser user) {
        return ApiResult.ok(access.menus(user.userId()));
    }

    @PutMapping("/user/password")
    public ApiResult<Boolean> changePassword(
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody ChangePasswordDTO request) {
        passwords.change(user.userId(), request.oldPassword(), request.newPassword());
        return ApiResult.ok(true);
    }
}
