package com.maplehush.admin.modules.system.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 新增角色请求数据 */
public record CreateRoleDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.-]{3,64}") String roleCode,
        @NotBlank @Size(max = 128) String roleName,
        @Size(max = 500) String description,
        @NotBlank String status,
        @Size(max = 1000) List<String> menuIds) {}
