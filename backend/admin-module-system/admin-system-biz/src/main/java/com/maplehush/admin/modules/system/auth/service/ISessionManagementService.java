package com.maplehush.admin.modules.system.auth.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.dto.SessionQueryDTO;
import com.maplehush.admin.modules.system.auth.vo.SessionVO;
import java.util.List;

/** 在线会话管理 */
public interface ISessionManagementService {
    public List<SessionVO> own(long userId);

    public List<SessionVO> own(long userId, long currentSessionId);

    public PageResult<SessionVO> all(long actorId, SessionQueryDTO query);

    public void revokeOwn(long userId, long sessionId);

    public void revokeManaged(long actorId, long sessionId);
}
