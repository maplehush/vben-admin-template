package com.maplehush.admin.modules.system.file.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectRequest;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.file.config.OssProperties;
import com.maplehush.admin.modules.system.file.service.impl.ImageUploadServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadServiceTest {
    private final OSS oss = mock(OSS.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<OSS> provider = mock(ObjectProvider.class);

    private final IImageUploadService uploads = new ImageUploadServiceImpl(
            provider,
            new OssProperties(
                    true,
                    "cn-beijing",
                    "https://oss-cn-beijing.aliyuncs.com",
                    "example-bucket",
                    "https://example-bucket.oss-cn-beijing.aliyuncs.com/",
                    "/vben-admin-img/",
                    "id",
                    "secret"));

    @Test
    void storesImageUnderConfiguredPrefixAndReturnsItsUrl() {
        when(provider.getIfAvailable()).thenReturn(oss);
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        var file = new MockMultipartFile("file", "unsafe.html", "text/html", png);

        var result = uploads.upload(42, file);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(oss).putObject(request.capture());
        assertEquals("example-bucket", request.getValue().getBucketName());
        assertEquals(result.key(), request.getValue().getKey());
        assertTrue(result.key().matches("vben-admin-img/42/\\d{4}-\\d{2}-\\d{2}/[a-f0-9-]{36}\\.png"));
        assertEquals("image/png", request.getValue().getMetadata().getContentType());
        assertEquals("https://example-bucket.oss-cn-beijing.aliyuncs.com/" + result.key(), result.url());
    }

    @Test
    void rejectsNonImagesBeforeSendingToOss() {
        when(provider.getIfAvailable()).thenReturn(oss);
        var file = new MockMultipartFile("file", "page.html", "image/png", "<script></script>".getBytes());

        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> uploads.upload(42, file)).code());
        verify(oss, never()).putObject(org.mockito.ArgumentMatchers.any(PutObjectRequest.class));
    }

    @Test
    void rejectsFilesAboveFiveMegabytes() {
        var file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[(int) IImageUploadService.MAX_IMAGE_BYTES + 1]);

        assertEquals(
                40000,
                assertThrows(ApiException.class, () -> uploads.upload(42, file)).code());
        verify(provider, never()).getIfAvailable();
    }
}
