package com.maplehush.admin.modules.system.menu.service;

import com.maplehush.admin.modules.system.menu.vo.MenuDetailVO;
import com.maplehush.admin.modules.system.menu.vo.MenuNodeVO;
import java.util.List;

/** 菜单树和菜单详情查询 */
public interface ISysMenuQueryService {
    public List<MenuNodeVO> tree(long actor);

    public MenuDetailVO detail(long actor, long id);
}
