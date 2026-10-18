package com.maplehush.admin.modules.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maplehush.admin.modules.system.dept.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 部门持久化及需要锁、级联约束的查询。 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
    /** 加锁读取部门的 ID、上级 ID 和状态，返回的实体只填充这三个字段 */
    SysDept lock(@Param("id") long id);

    Long lockParent(@Param("id") long id);

    Long parentOf(@Param("id") long id);

    long countActiveUsers(@Param("id") long id);

    long countUsers(@Param("id") long id);

    long countScopeReferences(@Param("id") long id);

    int setCode(@Param("id") long id, @Param("code") String code);

    int updateDetails(
            @Param("id") long id,
            @Param("parentId") Long parentId,
            @Param("name") String name,
            @Param("sortOrder") int sortOrder,
            @Param("status") int status,
            @Param("remark") String remark);
}
