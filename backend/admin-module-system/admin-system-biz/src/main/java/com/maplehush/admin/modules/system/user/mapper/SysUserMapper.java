package com.maplehush.admin.modules.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户管理写入及角色、部门约束查询。 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /** 加锁读取用户的 ID、部门 ID 和状态，返回的实体只填充这三个字段 */
    SysUser lock(@Param("id") long id);

    long countSuperRole(@Param("userId") long userId);

    Long lockSuperRole();

    Long superRoleId();

    long countActiveSuperUsers();

    long countActiveDepartment(@Param("id") long id);

    long countActiveRoles(@Param("ids") List<Long> ids);

    int updateDetails(SysUser user);

    int updateStatus(@Param("id") long id, @Param("status") int status);

    int resetPassword(@Param("id") long id, @Param("hash") String hash);

    int updateProfile(SysUser user);

    int deleteRoles(@Param("id") long id);

    int insertRole(@Param("userId") long userId, @Param("roleId") long roleId);
}
