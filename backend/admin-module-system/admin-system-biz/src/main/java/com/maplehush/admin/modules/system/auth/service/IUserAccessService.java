package com.maplehush.admin.modules.system.auth.service;

import com.maplehush.admin.modules.system.auth.vo.MenuRouteVO;
import com.maplehush.admin.modules.system.auth.vo.UserInfoVO;
import java.util.List;

/** 用户菜单与权限码查询 */
public interface IUserAccessService {
    public UserInfoVO info(long userId);

    public List<String> codes(long userId);

    public List<MenuRouteVO> menus(long userId);
}
