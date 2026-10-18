package com.maplehush.admin.modules.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 菜单持久化；行锁、跨表引用与条件更新放在 XML 中 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    /** 加锁读取菜单结构和状态，返回的实体仅含校验所需字段 */
    SysMenu lock(@Param("id") long id);

    /** 加锁读取上级菜单的类型与状态 */
    SysMenu lockParent(@Param("id") long id);

    Long parentOf(@Param("id") long id);

    Long lockPermissionNamespace();

    long countRoleReferences(@Param("id") long id);

    long countPermissionReferences(@Param("id") long id);

    long countPermissionCode(@Param("code") String code);

    int updateCore(
            @Param("id") long id,
            @Param("title") String title,
            @Param("icon") String icon,
            @Param("sortOrder") int sortOrder);

    int updateDetails(SysMenu menu);
}
