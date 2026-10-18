package com.maplehush.admin.modules.system.dept.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 新增部门请求数据 */
public record CreateDeptDTO(
        String parentId,
        @NotBlank @Size(max = 128) String deptName,
        @NotNull Integer sortOrder,
        @NotBlank String status,
        @Size(max = 500) String remark) {}
