package com.maplehush.admin.modules.system.dict.dto;

/** 字典项分页查询条件 */
public record ItemQueryDTO(int page, int pageSize, String itemLabel, String status) {}
