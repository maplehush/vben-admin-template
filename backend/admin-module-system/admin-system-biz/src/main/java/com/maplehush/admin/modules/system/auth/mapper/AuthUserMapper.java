package com.maplehush.admin.modules.system.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.user.entity.SysUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户认证查询及账户安全更新。 */
@Mapper
public interface AuthUserMapper extends BaseMapper<SysUser> {
    Long lockEnabledSuperAdminRole();

    int assignRole(@Param("userId") long userId, @Param("roleId") long roleId);

    SysUser findForLogin(@Param("username") String username);

    AuthenticatedSessionState findAuthenticatedUser(@Param("userId") long userId, @Param("sessionId") long sessionId);

    String lockPasswordHash(@Param("userId") long userId);

    int changePassword(@Param("userId") long userId, @Param("hash") String hash, @Param("now") LocalDateTime now);

    int recordLogin(@Param("userId") long userId, @Param("now") LocalDateTime now);
}
