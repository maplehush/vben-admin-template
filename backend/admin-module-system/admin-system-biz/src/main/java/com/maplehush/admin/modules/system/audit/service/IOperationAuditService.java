package com.maplehush.admin.modules.system.audit.service;

import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;

/** 在业务事务内记录成功的管理操作。 */
public interface IOperationAuditService {
    void record(long actor, String action, AuditTargetType targetType, long targetId);
}
