package com.maplehush.admin.modules.system.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.permission.entity.SysPermission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 接口权限持久化，列表筛选和跨表校验见 XML。 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    long countFiltered(@Param("pattern") String pattern);

    List<SysPermission> listFiltered(
            @Param("pattern") String pattern, @Param("limit") int limit, @Param("offset") int offset);

    long countRoleGrants(@Param("id") long id);

    long countEligibleMenu(@Param("id") long id);

    long countMenuCode(@Param("code") String code);

    Long lockPermissionNamespace();

    int updateDetails(
            @Param("id") long id,
            @Param("menuId") Long menuId,
            @Param("title") String title,
            @Param("sortOrder") int sortOrder,
            @Param("status") int status);
}
