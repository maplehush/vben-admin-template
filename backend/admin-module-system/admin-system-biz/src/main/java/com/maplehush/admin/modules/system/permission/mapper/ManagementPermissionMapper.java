package com.maplehush.admin.modules.system.permission.mapper;

import com.maplehush.admin.modules.system.dept.entity.SysDept;
import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import com.maplehush.admin.modules.system.role.entity.SysRole;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 权限判定和部门数据范围所需的持久化查询。 */
@Mapper
public interface ManagementPermissionMapper {
    long countSuperAdminRoles(@Param("actorId") long actorId);

    Long primaryDepartment(@Param("actorId") long actorId);

    List<SysRole> grantingRoles(@Param("actorId") long actorId, @Param("code") String code);

    SysMenu menuByCode(@Param("code") String code);

    SysMenu menuById(@Param("id") long id);

    List<Long> customDepartmentIds(@Param("roleId") long roleId);

    List<SysDept> departmentEdges();
}
