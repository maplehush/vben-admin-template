package com.maplehush.admin.modules.system.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 更新角色请求数据 */
public record UpdateRoleDTO(
        @NotBlank @Size(max = 128) String roleName,
        @Size(max = 500) String description,
        @NotBlank String status,
        @Size(max = 1000) List<String> menuIds) {}
