package com.maplehush.admin.modules.system.dict.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dict.dto.DictDataDTO;
import com.maplehush.admin.modules.system.dict.dto.DictQueryDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemDataDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemQueryDTO;
import com.maplehush.admin.modules.system.dict.service.ISysDictService;
import com.maplehush.admin.modules.system.dict.vo.DictVO;
import com.maplehush.admin.modules.system.dict.vo.ItemVO;
import com.maplehush.admin.modules.system.dict.vo.OptionVO;
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

/** 字典类型、字典项及启用选项接口 */
@RestController
public class SysDictController {
    private final ISysDictService service;

    public SysDictController(ISysDictService service) {
        this.service = service;
    }

    @GetMapping("/system/dicts")
    public ApiResult<PageResult<DictVO>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(service.list(actor.userId(), new DictQueryDTO(page, pageSize, dictCode, dictName, status)));
    }

    @GetMapping("/system/dicts/{dictId}")
    public ApiResult<DictVO> detail(@AuthenticationPrincipal CurrentUser actor, @PathVariable String dictId) {
        return ApiResult.ok(service.detail(actor.userId(), id(dictId)));
    }

    @PostMapping("/system/dicts")
    public ApiResult<Boolean> create(@AuthenticationPrincipal CurrentUser actor, @RequestBody DictDataDTO data) {
        service.create(actor.userId(), data);
        return ApiResult.ok(true);
    }

    @PutMapping("/system/dicts/{dictId}")
    public ApiResult<Boolean> update(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String dictId, @RequestBody DictDataDTO data) {
        service.update(actor.userId(), id(dictId), data);
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/dicts/{dictId}")
    public ApiResult<Boolean> delete(@AuthenticationPrincipal CurrentUser actor, @PathVariable String dictId) {
        service.delete(actor.userId(), id(dictId));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/dicts/{dictId}/items")
    public ApiResult<PageResult<ItemVO>> items(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String dictId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String itemLabel,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(
                service.items(actor.userId(), id(dictId), new ItemQueryDTO(page, pageSize, itemLabel, status)));
    }

    @PostMapping("/system/dicts/{dictId}/items")
    public ApiResult<Boolean> createItem(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String dictId, @RequestBody ItemDataDTO data) {
        service.createItem(actor.userId(), id(dictId), data);
        return ApiResult.ok(true);
    }

    @PutMapping("/system/dicts/{dictId}/items/{itemId}")
    public ApiResult<Boolean> updateItem(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable String dictId,
            @PathVariable String itemId,
            @RequestBody ItemDataDTO data) {
        service.updateItem(actor.userId(), id(dictId), id(itemId), data);
        return ApiResult.ok(true);
    }

    @DeleteMapping("/system/dicts/{dictId}/items/{itemId}")
    public ApiResult<Boolean> deleteItem(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable String dictId, @PathVariable String itemId) {
        service.deleteItem(actor.userId(), id(dictId), id(itemId));
        return ApiResult.ok(true);
    }

    @GetMapping("/system/dict-options/{code}")
    public ApiResult<List<OptionVO>> options(@PathVariable String code) {
        return ApiResult.ok(service.options(code));
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
