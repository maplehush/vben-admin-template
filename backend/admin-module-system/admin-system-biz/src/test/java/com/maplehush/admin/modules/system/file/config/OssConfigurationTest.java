package com.maplehush.admin.modules.system.file.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OssConfigurationTest {
    @Test
    void createsV4ClientWithoutConnectingToOss() {
        var properties = new OssProperties(
                true,
                "cn-beijing",
                "https://oss-cn-beijing.aliyuncs.com",
                "bucket",
                "https://bucket.oss-cn-beijing.aliyuncs.com",
                "vben-admin-img",
                "test-key",
                "test-secret");
        var client = new OssConfiguration().ossClient(properties);
        try {
            assertNotNull(client);
        } finally {
            client.shutdown();
        }
    }

    @Test
    void rejectsMissingCredentials() {
        var properties = new OssProperties(
                true,
                "cn-beijing",
                "https://oss-cn-beijing.aliyuncs.com",
                "bucket",
                "https://bucket.oss-cn-beijing.aliyuncs.com",
                "vben-admin-img",
                "",
                "");
        assertThrows(IllegalStateException.class, () -> new OssConfiguration().ossClient(properties));
    }
}
