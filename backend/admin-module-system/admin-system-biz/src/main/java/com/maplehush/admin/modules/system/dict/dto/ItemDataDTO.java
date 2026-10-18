package com.maplehush.admin.modules.system.dict.dto;

/** 字典项保存数据 */
public record ItemDataDTO(
        String itemValue, String itemLabel, Integer sortOrder, String status, String remark, String itemColor) {}
