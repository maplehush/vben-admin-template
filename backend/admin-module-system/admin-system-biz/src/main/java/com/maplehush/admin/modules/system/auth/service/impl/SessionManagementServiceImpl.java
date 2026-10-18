package com.maplehush.admin.modules.system.auth.service.impl;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.common.util.DateUtils;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.auth.dto.SessionQueryDTO;
import com.maplehush.admin.modules.system.auth.mapper.SysSessionMapper;
import com.maplehush.admin.modules.system.auth.service.ISessionManagementService;
import com.maplehush.admin.modules.system.auth.vo.SessionVO;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 查询和撤销在线会话；受保护账号的下线限制由数据库条件更新保证。 */
@Service
public class SessionManagementServiceImpl implements ISessionManagementService {
    private final Clock clock;
    private final SysSessionMapper sessions;
    private final IManagementPermissionService permissions;
    private final IOperationAuditService audit;

    public SessionManagementServiceImpl(
            SysSessionMapper sessions,
            IManagementPermissionService permissions,
            IOperationAuditService audit,
            Clock clock) {
        this.sessions = sessions;
        this.permissions = permissions;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public List<SessionVO> own(long userId) {
        return own(userId, -1);
    }

    public List<SessionVO> own(long userId, long currentSessionId) {
        return sessions.listOwn(userId, DateUtils.now(clock), currentSessionId);
    }

    @Override
    public PageResult<SessionVO> all(long actorId, SessionQueryDTO query) {
        permissions.require(actorId, "system:session:list");
        int page = query.page();
        int pageSize = query.pageSize();
        String username = query.username();
        if (page < 1
                || pageSize < 1
                || pageSize > 100
                || (long) (page - 1) * pageSize > Integer.MAX_VALUE
                || username != null && username.length() > 64) throw ApiException.invalidRequest();
        String filter = username == null || username.isBlank() ? null : username.trim();
        LocalDateTime now = DateUtils.now(clock);
        long total = sessions.countActive(now, filter);
        List<SessionVO> items = sessions.listActive(now, filter, pageSize, (page - 1) * pageSize);
        return new PageResult<>(items, total);
    }

    @Override
    @Transactional
    public void revokeOwn(long userId, long sessionId) {
        if (sessionId < 1) throw ApiException.invalidRequest();
        int changed = sessions.revokeOwn(userId, sessionId, DateUtils.now(clock));
        if (changed == 0) throw ApiException.notFound();
        audit.record(userId, "auth:session:revoke", AuditTargetType.SESSION, sessionId);
    }

    @Override
    @Transactional
    public void revokeManaged(long actorId, long sessionId) {
        permissions.require(actorId, "system:session:revoke");
        if (sessionId < 1) throw ApiException.invalidRequest();
        boolean protectSuperAdmin = !permissions.isSuperAdmin(actorId);
        int changed = sessions.revokeManaged(sessionId, DateUtils.now(clock), protectSuperAdmin);
        if (changed == 0) {
            if (protectSuperAdmin && sessions.isProtectedTarget(sessionId)) throw ApiException.forbidden();
            throw ApiException.notFound();
        }
        audit.record(actorId, "system:session:revoke", AuditTargetType.SESSION, sessionId);
    }
}
