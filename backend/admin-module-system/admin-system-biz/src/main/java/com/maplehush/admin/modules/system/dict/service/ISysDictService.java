package com.maplehush.admin.modules.system.dict.service;

import com.maplehush.admin.common.api.vo.PageResult;
import com.maplehush.admin.modules.system.dict.dto.DictDataDTO;
import com.maplehush.admin.modules.system.dict.dto.DictQueryDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemDataDTO;
import com.maplehush.admin.modules.system.dict.dto.ItemQueryDTO;
import com.maplehush.admin.modules.system.dict.vo.DictVO;
import com.maplehush.admin.modules.system.dict.vo.ItemVO;
import com.maplehush.admin.modules.system.dict.vo.OptionVO;
import java.util.List;

/** 字典类型与字典项业务服务 */
public interface ISysDictService {
    PageResult<DictVO> list(long actor, DictQueryDTO query);

    DictVO detail(long actor, long id);

    long create(long actor, DictDataDTO data);

    void update(long actor, long id, DictDataDTO data);

    void delete(long actor, long id);

    PageResult<ItemVO> items(long actor, long dictId, ItemQueryDTO query);

    long createItem(long actor, long dictId, ItemDataDTO data);

    void updateItem(long actor, long dictId, long itemId, ItemDataDTO data);

    void deleteItem(long actor, long dictId, long itemId);

    List<OptionVO> options(String code);
}
