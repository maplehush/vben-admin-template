package com.maplehush.admin.modules.system.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 菜单表实体；routeMeta 保存已校验的 JSON 文本 */
@Getter
@Setter
@TableName("sys_menu")
public class SysMenu {
    /** 菜单资源主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单资源 ID；为空表示根节点 */
    private Long parentId;

    /** 资源类型，如目录、菜单、按钮、内嵌页或外链 */
    private String menuType;

    /** 菜单资源的唯一标识 */
    private String menuKey;

    /** 前端路由名称 */
    private String routeName;

    /** 前端路由路径 */
    private String routePath;

    /** 前端页面组件路径 */
    private String component;

    /** 目录或菜单的重定向路径 */
    private String redirectPath;

    /** 按钮使用的权限编码 */
    private String authCode;

    /** 前端显示标题或国际化键 */
    private String titleKey;

    /** 菜单图标标识 */
    private String icon;

    /** 同级资源的显示顺序 */
    private Integer sortOrder;

    /** 已校验的路由扩展信息 JSON */
    private String routeMeta;

    /** 资源状态：1 为启用，0 为禁用 */
    private Integer status;

    /** 创建时间，按北京时间存储 */
    private LocalDateTime createTime;

    /** 最后修改时间，按北京时间存储 */
    private LocalDateTime updateTime;
}
