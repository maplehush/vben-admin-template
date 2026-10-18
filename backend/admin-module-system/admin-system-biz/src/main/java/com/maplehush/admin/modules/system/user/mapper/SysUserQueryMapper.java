package com.maplehush.admin.modules.system.user.mapper;

import com.maplehush.admin.modules.system.user.vo.UserDetailVO;
import com.maplehush.admin.modules.system.user.vo.UserSummaryVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户列表、详情及角色关系查询。 */
@Mapper
public interface SysUserQueryMapper {
    long countUsers(@Param("filter") SysUserQueryFilter filter);

    List<UserSummaryVO> listUsers(
            @Param("filter") SysUserQueryFilter filter, @Param("limit") int limit, @Param("offset") long offset);

    UserDetailVO findUser(@Param("filter") SysUserQueryFilter filter);

    long countUser(@Param("id") long id);

    List<Long> userRoleIds(@Param("id") long id);
}
