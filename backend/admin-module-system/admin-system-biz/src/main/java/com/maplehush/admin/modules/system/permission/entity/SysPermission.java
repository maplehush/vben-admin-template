package com.maplehush.admin.modules.system.permission.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 接口权限持久化实体 */
@Getter
@Setter
@TableName("sys_permission")
public class SysPermission {
    /** 接口权限主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的菜单资源 ID；为空表示独立接口权限 */
    private Long menuId;

    /** 权限资源的唯一标识 */
    private String permissionKey;

    /** 后端鉴权使用的权限编码 */
    private String permissionCode;

    /** 前端显示标题或国际化键 */
    private String titleKey;

    /** 权限资源的显示顺序 */
    private Integer sortOrder;

    /** 权限状态：1 为启用，0 为禁用 */
    private Integer status;

    /** 权限创建时间，按北京时间存储 */
    private LocalDateTime createTime;

    /** 权限最后修改时间，按北京时间存储 */
    private LocalDateTime updateTime;
}
