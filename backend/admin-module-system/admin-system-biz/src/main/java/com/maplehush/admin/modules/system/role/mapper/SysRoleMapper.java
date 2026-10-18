package com.maplehush.admin.modules.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.role.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 角色、菜单授权、接口授权及数据范围的持久化接口。 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    String lockRoleCode(@Param("id") long id);

    long countRole(@Param("id") long id);

    long countAssignedUsers(@Param("id") long id);

    long countActiveMenus(@Param("ids") List<Long> ids);

    long countActivePermissions(@Param("ids") List<Long> ids);

    long countActiveDepartments(@Param("ids") List<Long> ids);

    List<Long> menuIds(@Param("id") long id);

    List<Long> permissionIds(@Param("id") long id);

    List<Long> departmentIds(@Param("id") long id);

    String dataScopeType(@Param("id") long id);

    int updateDetails(
            @Param("id") long id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("status") int status);

    int updateDataScope(@Param("id") long id, @Param("type") String type);

    int deleteMenus(@Param("id") long id);

    int deletePermissions(@Param("id") long id);

    int deleteDepartments(@Param("id") long id);

    int insertMenu(@Param("roleId") long roleId, @Param("menuId") long menuId);

    int insertPermission(@Param("roleId") long roleId, @Param("permissionId") long permissionId);

    int insertDepartment(@Param("roleId") long roleId, @Param("deptId") long deptId);
}
