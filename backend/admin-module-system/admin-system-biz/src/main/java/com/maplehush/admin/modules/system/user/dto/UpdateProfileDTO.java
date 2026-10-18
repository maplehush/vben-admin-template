package com.maplehush.admin.modules.system.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 个人资料更新请求数据 */
public record UpdateProfileDTO(
        @NotBlank @Size(max = 128) String realName,
        @Email @Size(max = 255) String email,
        @Size(max = 32) String phone,
        @Size(max = 1024) String avatarUrl) {}
