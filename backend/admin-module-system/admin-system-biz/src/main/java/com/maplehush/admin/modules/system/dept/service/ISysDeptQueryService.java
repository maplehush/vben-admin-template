package com.maplehush.admin.modules.system.dept.service;

import com.maplehush.admin.modules.system.dept.dto.DeptQueryDTO;
import com.maplehush.admin.modules.system.dept.vo.DeptDetailVO;
import com.maplehush.admin.modules.system.dept.vo.DeptNodeVO;
import java.util.List;

/** 部门树和详情查询 */
public interface ISysDeptQueryService {
    public List<DeptNodeVO> departments(long actor, DeptQueryDTO query);

    public DeptDetailVO department(long actor, long id);
}
