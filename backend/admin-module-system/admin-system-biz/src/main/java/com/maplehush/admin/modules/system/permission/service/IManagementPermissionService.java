package com.maplehush.admin.modules.system.permission.service;

import java.util.Set;

/** 管理权限检查与可访问数据范围计算 */
public interface IManagementPermissionService {
    public void require(long actorId, String code);

    public void requireSuperAdmin(long actorId);

    public boolean isSuperAdmin(long actorId);

    public UserScope userScope(long actorId, String code);

    public record UserScope(boolean all, long actorId, Set<Long> deptIds) {}
}
