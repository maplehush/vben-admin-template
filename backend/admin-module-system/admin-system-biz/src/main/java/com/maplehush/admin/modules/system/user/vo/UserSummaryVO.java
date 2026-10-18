package com.maplehush.admin.modules.system.user.vo;

import com.maplehush.admin.modules.system.common.SystemStatus;
import java.time.LocalDateTime;

/** 用户列表项 */
public record UserSummaryVO(
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
        boolean systemAccount) {
    /** 将查询中的数据库值转换为接口字段 */
    public UserSummaryVO(
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
            boolean systemAccount) {
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
                systemAccount);
    }
}
