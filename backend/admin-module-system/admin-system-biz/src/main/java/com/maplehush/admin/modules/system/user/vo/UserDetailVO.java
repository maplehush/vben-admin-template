package com.maplehush.admin.modules.system.user.vo;

import com.maplehush.admin.modules.system.common.SystemStatus;
import java.time.LocalDateTime;
import java.util.List;

/** 用户详情 */
public record UserDetailVO(
        String id,
        String username,
        String realName,
        String remark,
        String email,
        String phone,
        String primaryDeptId,
        String primaryDeptName,
        String status,
        LocalDateTime createTime,
        boolean systemAccount,
        String avatarUrl,
        String homePath,
        List<String> roleIds,
        LocalDateTime updateTime) {
    /** MyBatis 先映射用户详情；角色 ID 随后从关系表读取 */
    public UserDetailVO(
            Long id,
            String username,
            String realName,
            String remark,
            String email,
            String phone,
            Long primaryDeptId,
            String primaryDeptName,
            Integer status,
            LocalDateTime createTime,
            boolean systemAccount,
            String avatarUrl,
            String homePath,
            LocalDateTime updateTime) {
        this(
                id.toString(),
                username,
                realName,
                remark,
                email,
                phone,
                primaryDeptId == null ? null : primaryDeptId.toString(),
                primaryDeptName,
                SystemStatus.apiValue(status),
                createTime,
                systemAccount,
                avatarUrl,
                homePath,
                List.of(),
                updateTime);
    }

    public UserDetailVO withRoleIds(List<String> ids) {
        return new UserDetailVO(
                id,
                username,
                realName,
                remark,
                email,
                phone,
                primaryDeptId,
                primaryDeptName,
                status,
                createTime,
                systemAccount,
                avatarUrl,
                homePath,
                ids,
                updateTime);
    }
}
