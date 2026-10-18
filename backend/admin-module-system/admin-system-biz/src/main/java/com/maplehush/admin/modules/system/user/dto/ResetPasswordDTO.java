package com.maplehush.admin.modules.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 重置密码请求数据 */
public record ResetPasswordDTO(
        @NotBlank @Size(min = 8, max = 128) String temporaryPassword) {}
