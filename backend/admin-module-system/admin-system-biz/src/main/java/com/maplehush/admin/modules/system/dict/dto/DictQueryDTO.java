package com.maplehush.admin.modules.system.dict.dto;

/** 字典类型分页查询条件 */
public record DictQueryDTO(int page, int pageSize, String dictCode, String dictName, String status) {}
