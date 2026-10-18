package com.maplehush.admin.modules.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 修改密码请求数据 */
public record ChangePasswordDTO(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword) {}
