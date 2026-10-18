package com.maplehush.admin.modules.system.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 登录请求数据 */
public record LoginDTO(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password,
        String captchaId,
        String captchaCode) {}
