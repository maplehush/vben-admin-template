package com.maplehush.admin.modules.system.audit.dto;

/** 操作日志查询条件 */
public record OperationLogQueryDTO(
        int page, int pageSize, String username, String actionCode, String startTime, String endTime) {}
