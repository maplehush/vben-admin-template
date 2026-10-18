package com.maplehush.admin.modules.system.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 用户角色分配请求数据 */
public record ReplaceUserRolesDTO(@NotNull @Size(max = 1000) List<String> roleIds) {}
