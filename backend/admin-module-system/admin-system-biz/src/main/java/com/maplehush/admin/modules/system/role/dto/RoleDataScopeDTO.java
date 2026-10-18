package com.maplehush.admin.modules.system.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 角色数据范围请求数据 */
public record RoleDataScopeDTO(
        @NotBlank String type, @NotNull @Size(max = 1000) List<String> deptIds) {}
