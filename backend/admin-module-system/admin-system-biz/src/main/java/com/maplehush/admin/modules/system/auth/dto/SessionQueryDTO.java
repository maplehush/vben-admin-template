package com.maplehush.admin.modules.system.auth.dto;

/** 在线会话查询条件 */
public record SessionQueryDTO(int page, int pageSize, String username) {}
