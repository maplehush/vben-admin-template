package com.maplehush.admin.modules.system.role.service;

import java.util.List;

/** 角色维护、菜单授权和数据范围设置 */
public interface ISysRoleService {
    public long create(long actor, String code, String name, String description, String status);

    public long create(long actor, String code, String name, String description, String status, List<Long> menuIds);

    public void update(long actor, long id, String name, String description, String status);

    public void update(long actor, long id, String name, String description, String status, List<Long> menuIds);

    public void delete(long actor, long id);

    public List<String> menus(long actor, long id);

    public void replaceMenus(long actor, long id, List<Long> ids);

    public List<String> permissions(long actor, long id);

    public void replacePermissions(long actor, long id, List<Long> ids);

    public DataScope dataScope(long actor, long id);

    public void updateDataScope(long actor, long id, String type, List<Long> deptIds);

    public record DataScope(String type, List<String> deptIds) {}
}
