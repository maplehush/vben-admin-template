package com.maplehush.admin.modules.system.dict.vo;

import java.time.LocalDateTime;

/** 字典项响应视图 */
public record ItemVO(
        String id,
        String dictId,
        String itemValue,
        String itemLabel,
        int sortOrder,
        String status,
        String remark,
        String itemColor,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
