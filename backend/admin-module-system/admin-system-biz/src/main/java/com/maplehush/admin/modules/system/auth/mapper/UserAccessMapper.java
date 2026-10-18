package com.maplehush.admin.modules.system.auth.mapper;

import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import com.maplehush.admin.modules.system.permission.entity.SysPermission;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 登录用户信息、菜单与独立接口权限查询。 */
@Mapper
public interface UserAccessMapper {
    SysUser userInfo(@Param("userId") long userId);

    List<String> roleCodes(@Param("userId") long userId);

    List<Long> grantedMenuIds(@Param("userId") long userId);

    List<Long> grantedPermissionIds(@Param("userId") long userId);

    List<SysMenu> activeMenus();

    List<SysPermission> activePermissions();
}
