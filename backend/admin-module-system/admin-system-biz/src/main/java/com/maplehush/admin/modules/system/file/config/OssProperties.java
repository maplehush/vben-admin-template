package com.maplehush.admin.modules.system.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oss")
public record OssProperties(
        boolean enabled,
        String region,
        String endpoint,
        String bucket,
        String domain,
        String prefix,
        String accessKeyId,
        String accessKeySecret) {}
