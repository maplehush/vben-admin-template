package com.maplehush.admin.modules.system.audit.dto;

/** 登录日志查询条件 */
public record LoginLogQueryDTO(
        int page, int pageSize, String username, Boolean success, String startTime, String endTime) {}
