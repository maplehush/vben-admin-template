package com.maplehush.admin.modules.system.role.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.role.dto.RoleQueryDTO;
import com.maplehush.admin.modules.system.role.vo.RoleDetailVO;
import com.maplehush.admin.modules.system.role.vo.RoleSummaryVO;

/** 角色列表和详情查询 */
public interface ISysRoleQueryService {
    PageResult<RoleSummaryVO> roles(long actor, RoleQueryDTO query);

    RoleDetailVO role(long actor, long id);
}
