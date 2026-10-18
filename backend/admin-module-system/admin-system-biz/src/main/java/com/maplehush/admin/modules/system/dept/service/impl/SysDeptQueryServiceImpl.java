package com.maplehush.admin.modules.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.dto.DeptQueryDTO;
import com.maplehush.admin.modules.system.dept.entity.SysDept;
import com.maplehush.admin.modules.system.dept.mapper.SysDeptMapper;
import com.maplehush.admin.modules.system.dept.service.ISysDeptQueryService;
import com.maplehush.admin.modules.system.dept.vo.DeptDetailVO;
import com.maplehush.admin.modules.system.dept.vo.DeptNodeVO;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SysDeptQueryServiceImpl implements ISysDeptQueryService {
    private final SysDeptMapper departments;
    private final IManagementPermissionService permissions;

    public SysDeptQueryServiceImpl(SysDeptMapper departments, IManagementPermissionService permissions) {
        this.departments = departments;
        this.permissions = permissions;
    }

    @Override
    public List<DeptNodeVO> departments(long actor, DeptQueryDTO query) {
        permissions.require(actor, "system:dept:list");
        Integer value = SystemStatus.databaseValueOrNull(query.status());
        return tree(rows(), value).roots();
    }

    @Override
    public DeptDetailVO department(long actor, long id) {
        permissions.require(actor, "system:dept:view");
        var rows = rows();
        SysDept row =
                rows.stream().filter(item -> item.getId() == id).findFirst().orElseThrow(ApiException::notFound);
        DeptNodeVO node = tree(rows, null).byId().get(id);
        return new DeptDetailVO(
                node.id(),
                node.parentId(),
                node.deptCode(),
                node.deptName(),
                node.sortOrder(),
                node.status(),
                node.remark(),
                node.children(),
                row.getCreateTime(),
                row.getUpdateTime());
    }

    private List<SysDept> rows() {
        return departments.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder, SysDept::getId));
    }

    private DeptTree tree(List<SysDept> rows, Integer status) {
        Map<Long, SysDept> source = new HashMap<>();
        rows.forEach(row -> source.put(row.getId(), row));
        Set<Long> included = new HashSet<>();
        for (SysDept row : rows) {
            if (status != null && row.getStatus() != status) continue;
            SysDept cursor = row;
            Set<Long> seen = new HashSet<>();
            while (cursor != null && seen.add(cursor.getId())) {
                included.add(cursor.getId());
                cursor = cursor.getParentId() == null ? null : source.get(cursor.getParentId());
            }
        }
        Map<Long, DeptNodeVO> byId = new HashMap<>();
        rows.stream()
                .filter(row -> included.contains(row.getId()))
                .forEach(row -> byId.put(
                        row.getId(),
                        new DeptNodeVO(
                                Long.toString(row.getId()),
                                row.getParentId() == null
                                        ? null
                                        : row.getParentId().toString(),
                                row.getDeptCode(),
                                row.getDeptName(),
                                row.getSortOrder(),
                                SystemStatus.apiValue(row.getStatus()),
                                row.getRemark(),
                                row.getCreateTime(),
                                new ArrayList<>())));
        List<DeptNodeVO> roots = new ArrayList<>();
        rows.stream()
                .filter(row -> included.contains(row.getId()))
                .sorted(Comparator.comparingInt(SysDept::getSortOrder).thenComparingLong(SysDept::getId))
                .forEach(row -> {
                    DeptNodeVO node = byId.get(row.getId());
                    DeptNodeVO parent = row.getParentId() == null ? null : byId.get(row.getParentId());
                    if (parent == null) roots.add(node);
                    else parent.children().add(node);
                });
        return new DeptTree(roots, byId);
    }

    private record DeptTree(List<DeptNodeVO> roots, Map<Long, DeptNodeVO> byId) {}
}
