package com.maplehush.admin.modules.system.dict.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 字典项表实体 */
@Getter
@Setter
@TableName("sys_dict_item")
public class SysDictItem {
    /** 字典项主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属字典 ID */
    private Long dictId;

    /** 字典项取值，同一字典内唯一 */
    private String itemValue;

    /** 字典项显示文本 */
    private String itemLabel;

    /** 同一字典内的显示顺序 */
    private Integer sortOrder;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 展示颜色，空值表示不设置颜色 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String itemColor;

    /** 备注 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    /** 创建时间，北京时间 */
    private LocalDateTime createTime;

    /** 更新时间，北京时间 */
    private LocalDateTime updateTime;
}
