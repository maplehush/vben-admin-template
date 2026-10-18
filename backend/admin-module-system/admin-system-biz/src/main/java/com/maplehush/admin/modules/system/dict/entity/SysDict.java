package com.maplehush.admin.modules.system.dict.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 字典类型表实体 */
@Getter
@Setter
@TableName("sys_dict")
public class SysDict {
    /** 字典主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典编码，系统内唯一 */
    private String dictCode;

    /** 字典名称 */
    private String dictName;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 是否系统内置：1 是，0 否 */
    private Integer builtIn;

    /** 备注 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /** 创建时间，北京时间 */
    private LocalDateTime createTime;

    /** 更新时间，北京时间 */
    private LocalDateTime updateTime;
}
