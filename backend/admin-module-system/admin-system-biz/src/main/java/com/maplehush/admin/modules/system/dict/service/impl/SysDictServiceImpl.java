package com.maplehush.admin.modules.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.common.constant.CommonStatus;
import com.maplehush.admin.modules.system.audit.constant.AuditTargetType;
import com.maplehush.admin.modules.system.audit.service.IOperationAuditService;
import com.maplehush.admin.modules.system.common.SystemStatus;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.dict.dto.DictDataDTO;
import com.maplehush.admin.modules.system.dict.dto.DictQueryDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemDataDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemQueryDTO;
import com.maplehush.admin.modules.system.dict.entity.SysDict;
import com.maplehush.admin.modules.system.dict.entity.SysDictItem;
import com.maplehush.admin.modules.system.dict.mapper.SysDictItemMapper;
import com.maplehush.admin.modules.system.dict.mapper.SysDictMapper;
import com.maplehush.admin.modules.system.dict.service.ISysDictService;
import com.maplehush.admin.modules.system.dict.vo.DictVO;
import com.maplehush.admin.modules.system.dict.vo.ItemVO;
import com.maplehush.admin.modules.system.dict.vo.OptionVO;
import com.maplehush.admin.modules.system.permission.service.IManagementPermissionService;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 字典管理；系统内置状态项的取值由 CommonStatus 定义 */
@Service
public class SysDictServiceImpl implements ISysDictService {
    private static final Pattern CODE = Pattern.compile("[a-z][a-z0-9_]{1,63}");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Set<String> ITEM_COLORS = Set.of("success", "warning", "error", "processing");
    private final SysDictMapper dicts;
    private final SysDictItemMapper items;
    private final IManagementPermissionService permissions;
    private final IOperationAuditService audit;

    public SysDictServiceImpl(
            SysDictMapper dicts,
            SysDictItemMapper items,
            IManagementPermissionService permissions,
            IOperationAuditService audit) {
        this.dicts = dicts;
        this.items = items;
        this.permissions = permissions;
        this.audit = audit;
    }

    @Override
    public PageResult<DictVO> list(long actor, DictQueryDTO criteria) {
        permissions.require(actor, "system:dict:list");
        checkPage(criteria.page(), criteria.pageSize());
        var query = new LambdaQueryWrapper<SysDict>().orderByAsc(SysDict::getId);
        String code = like(criteria.dictCode());
        String name = like(criteria.dictName());
        if (code != null) query.like(SysDict::getDictCode, code);
        if (name != null) query.like(SysDict::getDictName, name);
        if (criteria.status() != null) query.eq(SysDict::getStatus, SystemStatus.databaseValue(criteria.status()));
        var result = dicts.selectPage(new Page<>(criteria.page(), criteria.pageSize()), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(), result.getTotal());
    }

    @Override
    public DictVO detail(long actor, long id) {
        permissions.require(actor, "system:dict:view");
        return view(dict(id));
    }

    @Override
    @Transactional
    public long create(long actor, DictDataDTO data) {
        permissions.require(actor, "system:dict:create");
        validate(data);
        SysDict row = new SysDict();
        row.setDictCode(data.dictCode());
        row.setDictName(data.dictName().trim());
        row.setStatus(SystemStatus.databaseValue(data.status()));
        row.setBuiltIn(0);
        row.setRemark(data.remark());
        try {
            dicts.insert(row);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
        audit.record(actor, "system:dict:create", AuditTargetType.DICT, row.getId());
        return row.getId();
    }

    @Override
    @Transactional
    public void update(long actor, long id, DictDataDTO data) {
        permissions.require(actor, "system:dict:update");
        validate(data);
        SysDict current = dict(id);
        if (current.getBuiltIn() == 1) throw ApiException.forbidden();
        if (!current.getDictCode().equals(data.dictCode())) throw ApiException.invalidRequest();
        SysDict patch = new SysDict();
        patch.setId(id);
        patch.setDictName(data.dictName().trim());
        patch.setStatus(SystemStatus.databaseValue(data.status()));
        patch.setRemark(data.remark());
        dicts.updateById(patch);
        audit.record(actor, "system:dict:update", AuditTargetType.DICT, id);
    }

    @Override
    @Transactional
    public void delete(long actor, long id) {
        permissions.require(actor, "system:dict:delete");
        SysDict current = dict(id);
        if (current.getBuiltIn() == 1) throw ApiException.forbidden();
        if (items.selectCount(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id)) > 0)
            throw ApiException.conflict();
        try {
            dicts.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
        audit.record(actor, "system:dict:delete", AuditTargetType.DICT, id);
    }

    @Override
    public PageResult<ItemVO> items(long actor, long dictId, ItemQueryDTO criteria) {
        permissions.require(actor, "system:dict:view");
        dict(dictId);
        checkPage(criteria.page(), criteria.pageSize());
        var query = new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dictId)
                .orderByAsc(SysDictItem::getSortOrder, SysDictItem::getId);
        String label = like(criteria.itemLabel());
        if (label != null) query.like(SysDictItem::getItemLabel, label);
        if (criteria.status() != null) query.eq(SysDictItem::getStatus, SystemStatus.databaseValue(criteria.status()));
        var result = items.selectPage(new Page<>(criteria.page(), criteria.pageSize()), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(), result.getTotal());
    }

    @Override
    @Transactional
    public long createItem(long actor, long dictId, ItemDataDTO data) {
        permissions.require(actor, "system:dict:create");
        validate(data);
        SysDict parent = dict(dictId);
        if (parent.getBuiltIn() == 1) throw ApiException.forbidden();
        SysDictItem row = new SysDictItem();
        row.setDictId(dictId);
        setItem(row, data);
        try {
            items.insert(row);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict();
        }
        audit.record(actor, "system:dict-item:create", AuditTargetType.DICT_ITEM, row.getId());
        return row.getId();
    }

    @Override
    @Transactional
    public void updateItem(long actor, long dictId, long itemId, ItemDataDTO data) {
        permissions.require(actor, "system:dict:update");
        validate(data);
        if (dict(dictId).getBuiltIn() == 1) throw ApiException.forbidden();
        SysDictItem row = item(dictId, itemId);
        if (!row.getItemValue().equals(data.itemValue())) throw ApiException.invalidRequest();
        SysDictItem patch = new SysDictItem();
        patch.setId(itemId);
        setItem(patch, data);
        items.updateById(patch);
        audit.record(actor, "system:dict-item:update", AuditTargetType.DICT_ITEM, itemId);
    }

    @Override
    @Transactional
    public void deleteItem(long actor, long dictId, long itemId) {
        permissions.require(actor, "system:dict:delete");
        if (dict(dictId).getBuiltIn() == 1) throw ApiException.forbidden();
        item(dictId, itemId);
        items.deleteById(itemId);
        audit.record(actor, "system:dict-item:delete", AuditTargetType.DICT_ITEM, itemId);
    }

    @Override
    public List<OptionVO> options(String code) {
        if (code == null || !CODE.matcher(code).matches()) throw ApiException.invalidRequest();
        SysDict row = dicts.selectOne(new LambdaQueryWrapper<SysDict>().eq(SysDict::getDictCode, code));
        if (row == null || row.getStatus() != CommonStatus.ENABLED.databaseValue()) throw ApiException.notFound();
        return itemRows(row.getId()).stream()
                .filter(item -> item.getStatus() == CommonStatus.ENABLED.databaseValue())
                .map(item -> new OptionVO(item.getItemValue(), item.getItemLabel(), item.getItemColor()))
                .toList();
    }

    private SysDict dict(long id) {
        SysDict row = dicts.selectById(id);
        if (row == null) throw ApiException.notFound();
        return row;
    }

    private SysDictItem item(long dictId, long itemId) {
        SysDictItem row = items.selectById(itemId);
        if (row == null || row.getDictId() != dictId) throw ApiException.notFound();
        return row;
    }

    private List<SysDictItem> itemRows(long dictId) {
        return items.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictId, dictId)
                .orderByAsc(SysDictItem::getSortOrder, SysDictItem::getId));
    }

    private void checkPage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100 || (long) (page - 1) * pageSize > Integer.MAX_VALUE)
            throw ApiException.invalidRequest();
    }

    private String like(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > 128) throw ApiException.invalidRequest();
        return value.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private void setItem(SysDictItem row, ItemDataDTO data) {
        row.setItemValue(data.itemValue());
        row.setItemLabel(data.itemLabel().trim());
        row.setSortOrder(data.sortOrder());
        row.setStatus(SystemStatus.databaseValue(data.status()));
        row.setRemark(data.remark());
        row.setItemColor(data.itemColor());
    }

    private void validate(DictDataDTO data) {
        if (data == null
                || data.dictCode() == null
                || !CODE.matcher(data.dictCode()).matches()
                || data.dictName() == null
                || data.dictName().isBlank()
                || data.dictName().length() > 128
                || data.remark() != null && data.remark().length() > 500) throw ApiException.invalidRequest();
        SystemStatus.databaseValue(data.status());
    }

    private void validate(ItemDataDTO data) {
        if (data == null
                || data.itemValue() == null
                || data.itemValue().isBlank()
                || data.itemValue().length() > 128
                || data.itemLabel() == null
                || data.itemLabel().isBlank()
                || data.itemLabel().length() > 128
                || data.sortOrder() == null
                || data.remark() != null && data.remark().length() > 500
                || data.itemColor() != null
                        && !ITEM_COLORS.contains(data.itemColor())
                        && !HEX_COLOR.matcher(data.itemColor()).matches()) throw ApiException.invalidRequest();
        SystemStatus.databaseValue(data.status());
    }

    private DictVO view(SysDict row) {
        return new DictVO(
                Long.toString(row.getId()),
                row.getDictCode(),
                row.getDictName(),
                SystemStatus.apiValue(row.getStatus()),
                row.getBuiltIn() == 1,
                row.getRemark(),
                row.getCreateTime(),
                row.getUpdateTime());
    }

    private ItemVO view(SysDictItem row) {
        return new ItemVO(
                Long.toString(row.getId()),
                Long.toString(row.getDictId()),
                row.getItemValue(),
                row.getItemLabel(),
                row.getSortOrder(),
                SystemStatus.apiValue(row.getStatus()),
                row.getRemark(),
                row.getItemColor(),
                row.getCreateTime(),
                row.getUpdateTime());
    }
}
