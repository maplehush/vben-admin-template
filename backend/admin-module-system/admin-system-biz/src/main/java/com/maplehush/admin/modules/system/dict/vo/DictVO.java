package com.maplehush.admin.modules.system.dict.vo;

import java.time.LocalDateTime;

/** 字典类型响应视图 */
public record DictVO(
        String id,
        String dictCode,
        String dictName,
        String status,
        boolean builtIn,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {}
