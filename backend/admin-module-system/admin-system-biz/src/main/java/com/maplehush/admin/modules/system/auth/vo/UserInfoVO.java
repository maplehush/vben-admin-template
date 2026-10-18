package com.maplehush.admin.modules.system.auth.vo;

import java.util.List;

/** 当前用户信息响应视图 */
public record UserInfoVO(
        String userId,
        String username,
        String realName,
        String email,
        String phone,
        String avatar,
        List<String> roles,
        String homePath,
        String desc,
        boolean mustChangePassword) {}
