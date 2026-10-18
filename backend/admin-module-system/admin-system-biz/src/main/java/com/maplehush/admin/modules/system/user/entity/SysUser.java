package com.maplehush.admin.modules.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 系统用户持久化实体 */
@Getter
@Setter
@TableName("sys_user")
public class SysUser {
    /** 用户主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名，系统内唯一 */
    private String username;

    /** 密码哈希，不保存明文密码 */
    private String passwordHash;

    /** 用户姓名 */
    private String realName;

    /** 用户备注 */
    private String remark;

    /** 电子邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 所属部门 ID；初始化的超级管理员可以为空 */
    private Long primaryDeptId;

    /** 头像图片地址 */
    private String avatarUrl;

    /** 登录后优先打开的首页路径 */
    private String homePath;

    /** 账号状态：1 为启用，0 为禁用 */
    private Integer status;

    /** 是否必须修改初始密码：1 为是，0 为否 */
    private Integer mustChangePassword;
}
