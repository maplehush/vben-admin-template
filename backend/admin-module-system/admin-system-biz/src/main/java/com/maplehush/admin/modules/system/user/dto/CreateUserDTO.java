package com.maplehush.admin.modules.system.user.dto;

import com.maplehush.admin.modules.system.common.web.RequestIds;
import com.maplehush.admin.modules.system.user.service.ISysUserService.UserData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 新增用户请求数据 */
public record CreateUserDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,64}") String username,
        @NotBlank @Size(max = 128) String realName,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String phone,
        @NotBlank String primaryDeptId,
        @Size(max = 1024) String avatarUrl,
        @Size(max = 255) String homePath,
        @Size(max = 500) String remark,
        @NotBlank String status,
        @NotNull @Size(max = 1000) List<String> roleIds) {
    public UserData data() {
        return new UserData(realName, email, phone, RequestIds.required(primaryDeptId), avatarUrl, homePath, remark);
    }
}
