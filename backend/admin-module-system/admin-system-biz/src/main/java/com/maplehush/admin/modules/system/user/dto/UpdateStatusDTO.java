package com.maplehush.admin.modules.system.user.dto;

import jakarta.validation.constraints.NotBlank;

/** 用户状态更新请求数据 */
public record UpdateStatusDTO(@NotBlank String status) {}
