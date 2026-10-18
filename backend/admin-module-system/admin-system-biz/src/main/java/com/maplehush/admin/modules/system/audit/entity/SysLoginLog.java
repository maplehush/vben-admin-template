package com.maplehush.admin.modules.system.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 登录日志持久化实体 */
@Getter
@Setter
@TableName("sys_login_log")
public class SysLoginLog {
    /** 日志主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户 ID，失败时可能为空 */
    private Long userId;

    /** 本次尝试的用户名 */
    private String username;

    /** 是否登录成功：1 为成功，0 为失败 */
    private Boolean success;

    /** 登录失败原因 */
    private String failureReason;

    /** 请求 IP 地址 */
    private String ipAddress;

    /** 客户端 User-Agent */
    private String userAgent;

    /** 发生时间，按北京时间存储 */
    private LocalDateTime occurTime;
}
