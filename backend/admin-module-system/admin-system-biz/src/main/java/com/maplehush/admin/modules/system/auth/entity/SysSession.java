package com.maplehush.admin.modules.system.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 登录会话持久化实体，对应 sys_session */
@Getter
@Setter
@TableName("sys_session")
public class SysSession {
    /** 会话主键，由数据库生成 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户 ID
     * */
    private Long userId;

    /** 刷新令牌的 SHA-256 摘要，不保存令牌明文 */
    private String refreshTokenHash;

    /** 登录设备信息，来源于请求的 User-Agent */
    private String deviceInfo;

    /** 登录时的客户端 IP 地址 */
    private String ipAddress;

    /** 刷新令牌到期时间，按北京时间存储 */
    private LocalDateTime expireTime;

    /** 会话撤销时间；为空表示尚未撤销 */
    private LocalDateTime revokeTime;

    /** 会话创建时间，按北京时间存储 */
    private LocalDateTime createTime;
}
