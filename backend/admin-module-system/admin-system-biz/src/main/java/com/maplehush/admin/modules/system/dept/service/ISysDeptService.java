package com.maplehush.admin.modules.system.dept.service;

/** 部门树维护与状态约束 */
public interface ISysDeptService {
    public long create(long actor, Long parentId, String name, int sortOrder, String status, String remark);

    public void update(long actor, long id, Long parentId, String name, int sortOrder, String status, String remark);

    public void delete(long actor, long id);
}
