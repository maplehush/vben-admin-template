package com.maplehush.admin.modules.system.role.vo;

import java.util.List;

/** 角色数据范围 */
public record RoleDataScopeVO(String type, List<String> deptIds) {}
