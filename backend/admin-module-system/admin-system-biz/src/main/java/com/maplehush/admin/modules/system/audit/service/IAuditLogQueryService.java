package com.maplehush.admin.modules.system.audit.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.audit.dto.LoginLogQueryDTO;
import com.maplehush.admin.modules.system.audit.dto.OperationLogQueryDTO;
import com.maplehush.admin.modules.system.audit.vo.LoginLogVO;
import com.maplehush.admin.modules.system.audit.vo.OperationLogVO;

/** 登录日志与操作日志查询 */
public interface IAuditLogQueryService {
    public PageResult<LoginLogVO> loginLogs(long actorId, LoginLogQueryDTO query);

    public PageResult<OperationLogVO> operationLogs(long actorId, OperationLogQueryDTO query);
}
