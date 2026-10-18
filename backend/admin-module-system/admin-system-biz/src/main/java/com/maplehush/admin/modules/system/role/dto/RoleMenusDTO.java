package com.maplehush.admin.modules.system.role.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 角色菜单授权请求 */
public record RoleMenusDTO(@NotNull @Size(max = 1000) List<String> menuIds) {}
