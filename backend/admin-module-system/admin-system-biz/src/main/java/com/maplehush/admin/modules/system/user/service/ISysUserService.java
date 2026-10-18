package com.maplehush.admin.modules.system.user.service;

import java.util.List;

/** 用户维护、状态控制和角色分配 */
public interface ISysUserService {
    public long create(long actor, CreateUser request);

    public void update(long actor, long targetId, UserData data);

    public void changeStatus(long actor, long targetId, String status);

    public void replaceRoles(long actor, long targetId, List<Long> roleIds);

    public void resetPassword(long actor, long targetId, String temporaryPassword);

    public void updateProfile(long actor, ProfileData data);

    public record UserData(
            String realName,
            String email,
            String phone,
            Long primaryDeptId,
            String avatarUrl,
            String homePath,
            String remark) {
        public UserData(
                String realName, String email, String phone, Long primaryDeptId, String avatarUrl, String homePath) {
            this(realName, email, phone, primaryDeptId, avatarUrl, homePath, null);
        }
    }

    public record ProfileData(String realName, String email, String phone, String avatarUrl) {}

    public record CreateUser(String username, UserData data, String status, List<Long> roleIds) {}
}
