package com.maplehush.admin.modules.system.user.dto;

import com.maplehush.admin.modules.system.common.web.RequestIds;
import com.maplehush.admin.modules.system.user.service.ISysUserService.UserData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 更新用户请求数据 */
public record UpdateUserDTO(
        @NotBlank @Size(max = 128) String realName,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String phone,
        String primaryDeptId,
        @Size(max = 1024) String avatarUrl,
        @Size(max = 255) String homePath,
        @Size(max = 500) String remark) {
    public UserData data() {
        return new UserData(
                realName,
                email,
                phone,
                primaryDeptId == null ? null : RequestIds.required(primaryDeptId),
                avatarUrl,
                homePath,
                remark);
    }
}
