package com.maplehush.admin.modules.system.audit.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 写入系统操作日志。 */
@Mapper
public interface OperationAuditMapper {
    int insert(
            @Param("actor") long actor,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("target") String target,
            @Param("requestId") String requestId,
            @Param("ipAddress") String ipAddress);
}
