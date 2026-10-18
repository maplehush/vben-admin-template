package com.maplehush.admin.modules.system.dept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 部门表实体；编号在取得数据库 ID 后生成 */
@Getter
@Setter
@TableName("sys_dept")
public class SysDept {
    /** 部门主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级部门 ID；为空表示根部门 */
    private Long parentId;

    /** 部门编号，由服务端根据主键生成 */
    private String deptCode;

    /** 部门名称 */
    private String deptName;

    /** 同级部门的显示顺序 */
    private Integer sortOrder;

    /** 部门状态：1 为启用，0 为禁用 */
    private Integer status;

    /** 部门备注 */
    private String remark;

    /** 创建时间，按北京时间存储 */
    private LocalDateTime createTime;

    /** 最后修改时间，按北京时间存储 */
    private LocalDateTime updateTime;
}
