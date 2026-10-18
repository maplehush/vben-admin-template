package com.maplehush.admin.modules.system.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 登录审计写入；失败记录独立于登录事务。 */
@Mapper
public interface AuthLogMapper {
    int insertLoginAttempt(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("success") boolean success,
            @Param("reason") String reason,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent);
}
