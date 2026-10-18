package com.maplehush.admin.modules.system.audit.service.impl;

import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.mapper.OperationAuditMapper;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.auth.config.RequestMetadataFilter;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 将操作日志写入与业务更新相同的事务。 */
@Service
public class OperationAuditServiceImpl implements IOperationAuditService {
    private final OperationAuditMapper mapper;

    public OperationAuditServiceImpl(OperationAuditMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(long actor, String action, AuditTargetType targetType, long targetId) {
        var attributes = RequestContextHolder.getRequestAttributes();
        String requestId = null;
        String ipAddress = null;
        if (attributes instanceof ServletRequestAttributes servlet) {
            requestId = (String) servlet.getRequest().getAttribute(RequestMetadataFilter.REQUEST_ID_ATTRIBUTE);
            ipAddress = servlet.getRequest().getRemoteAddr();
        }
        mapper.insert(actor, action, targetType.name(), Long.toString(targetId), requestId, ipAddress);
    }
}
