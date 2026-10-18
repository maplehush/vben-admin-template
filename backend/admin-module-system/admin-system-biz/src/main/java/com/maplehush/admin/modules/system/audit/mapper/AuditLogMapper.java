package com.maplehush.admin.modules.system.audit.mapper;

import com.maplehush.admin.modules.system.audit.entity.SysLoginLog;
import com.maplehush.admin.modules.system.audit.vo.OperationLogVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 登录与操作日志的权限范围查询。 */
@Mapper
public interface AuditLogMapper {
    long countLogin(@Param("filter") AuditFilter filter);

    List<SysLoginLog> listLogin(
            @Param("filter") AuditFilter filter, @Param("limit") int limit, @Param("offset") int offset);

    long countOperation(@Param("filter") AuditFilter filter);

    List<OperationLogVO> listOperation(
            @Param("filter") AuditFilter filter, @Param("limit") int limit, @Param("offset") int offset);
}
