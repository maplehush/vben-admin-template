package com.maplehush.admin.modules.system.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 角色表实体 */
@Getter
@Setter
@TableName("sys_role")
public class SysRole {
    /** 角色主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码，系统内唯一 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色备注 */
    private String description;

    /** 角色状态：1 为启用，0 为禁用 */
    private Integer status;

    /** 数据范围类型，决定该角色可访问的部门与用户数据 */
    private String dataScopeType;

    /** 创建时间，按北京时间存储 */
    private LocalDateTime createTime;

    /** 最后修改时间，按北京时间存储 */
    private LocalDateTime updateTime;
}
