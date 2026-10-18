package com.maplehush.admin.modules.system.permission.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.permission.dto.PermissionQueryDTO;
import com.maplehush.admin.modules.system.permission.vo.PermissionVO;

/** 接口权限资源维护 */
public interface ISysPermissionService {
    public PageResult<PermissionVO> list(long actor, PermissionQueryDTO query);

    public PermissionVO detail(long actor, long id);

    public long create(long actor, String key, String code, PermissionData data);

    public void update(long actor, long id, PermissionData data);

    public void delete(long actor, long id);

    public record PermissionData(Long menuId, String titleKey, int sortOrder, String status) {}
}
