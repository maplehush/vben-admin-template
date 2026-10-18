package com.maplehush.admin.modules.system.dept.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 部门响应视图 */
public record DeptNodeVO(
        String id,
        String parentId,
        String deptCode,
        String deptName,
        int sortOrder,
        String status,
        String remark,
        LocalDateTime createTime,
        List<DeptNodeVO> children) {}
