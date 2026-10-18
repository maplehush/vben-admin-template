package com.maplehush.admin.modules.system.file.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OssProperties.class)
public class OssConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "app.oss.enabled", havingValue = "true")
    OSS ossClient(OssProperties properties) {
        if (blank(properties.accessKeyId())
                || blank(properties.accessKeySecret())
                || blank(properties.region())
                || blank(properties.endpoint())
                || blank(properties.bucket())
                || blank(properties.domain())
                || blank(properties.prefix())) {
            throw new IllegalStateException("OSS is enabled but its configuration is incomplete");
        }
        ClientBuilderConfiguration clientConfiguration = new ClientBuilderConfiguration();
        clientConfiguration.setSignatureVersion(SignVersion.V4);
        return OSSClientBuilder.create()
                .endpoint(properties.endpoint())
                .region(properties.region())
                .credentialsProvider(
                        new DefaultCredentialProvider(properties.accessKeyId(), properties.accessKeySecret()))
                .clientConfiguration(clientConfiguration)
                .build();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
