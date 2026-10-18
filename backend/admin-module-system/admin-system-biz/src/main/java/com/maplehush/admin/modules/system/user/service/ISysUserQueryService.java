package com.maplehush.admin.modules.system.user.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.user.dto.UserQueryDTO;
import com.maplehush.admin.modules.system.user.vo.UserDetailVO;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;

/** 用户列表和详情查询 */
public interface ISysUserQueryService {
    PageResult<UserSummaryVO> users(long actor, UserQueryDTO query);

    UserDetailVO user(long actor, long id);
}
