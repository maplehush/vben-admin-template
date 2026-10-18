package com.maplehush.admin.modules.system.dept.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dept.dto.CreateDeptDTO;
import com.maplehush.admin.modules.system.dept.dto.DeptQueryDTO;
import com.maplehush.admin.modules.system.dept.dto.UpdateDeptDTO;
import com.maplehush.admin.modules.system.dept.service.ISysDeptQueryService;
import com.maplehush.admin.modules.system.dept.service.ISysDeptService;
import com.maplehush.admin.modules.system.dept.vo.DeptDetailVO;
import com.maplehush.admin.modules.system.dept.vo.DeptNodeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SysDeptController {
    private final ISysDeptService departments;
    private final ISysDeptQueryService queries;

    public SysDeptController(ISysDeptService departments, ISysDeptQueryService queries) {
        this.departments = departments;
        this.queries = queries;
    }

    @PostMapping("/system/depts")
    public ApiResult<Boolean> create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody CreateDeptDTO request) {
        departments.create(
                actor.userId(),
                idOrNull(request.parentId()),
                request.deptName(),
                request.sortOrder(),
                request.status(),
                request.remark());
        return ApiResult.ok(true);
    }

    @PutMapping("/system/depts/{deptId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String deptId,
            @Valid @RequestBody UpdateDeptDTO request) {
        departments.update(
                actor.userId(),
                id(deptId),
                idOrNull(request.parentId()),
                request.deptName(),
                request.sortOrder(),
                request.status(),
                request.remark());
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/depts/{deptId}")
    public ApiResult<Boolean> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable String deptId) {
        departments.delete(actor.userId(), id(deptId));
        return ApiResult.ok(true);
    }

    private Long idOrNull(String value) {
        return value == null ? null : id(value);
    }

    @GetMapping("/system/depts/tree")
    public ApiResult<List<DeptNodeVO>> departments(
            @AuthenticationPrincipal CurrentUser actor, @RequestParam(required = false) String status) {
        return ApiResult.ok(queries.departments(actor.userId(), new DeptQueryDTO(status)));
    }

    @GetMapping("/system/depts/{deptId}")
    public ApiResult<DeptDetailVO> department(@AuthenticationPrincipal CurrentUser actor, @PathVariable String deptId) {
        return ApiResult.ok(queries.department(actor.userId(), id(deptId)));
    }

    private long id(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) throw ApiException.invalidRequest();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw ApiException.invalidRequest();
        }
    }
}
