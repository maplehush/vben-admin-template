package com.maplehush.admin.modules.system.file.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.maplehush.admin.modules.system.common.exception.ApiException;
import com.maplehush.admin.modules.system.file.config.OssProperties;
import com.maplehush.admin.modules.system.file.service.IImageUploadService;
import com.maplehush.admin.modules.system.file.vo.UploadedImageVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 校验图片并上传至配置的对象存储路径。 */
@Service
public class ImageUploadServiceImpl implements IImageUploadService {

    private final ObjectProvider<OSS> oss;
    private final OssProperties properties;

    public ImageUploadServiceImpl(ObjectProvider<OSS> oss, OssProperties properties) {
        this.oss = oss;
        this.properties = properties;
    }

    @Override
    public UploadedImageVO upload(long userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw ApiException.invalidRequest();
        }
        OSS client = oss.getIfAvailable();
        if (client == null) {
            throw ApiException.uploadUnavailable();
        }
        try {
            byte[] content = file.getBytes();
            ImageType type = ImageType.detect(content);
            if (type == null || content.length > MAX_IMAGE_BYTES) {
                throw ApiException.invalidRequest();
            }
            String prefix = properties.prefix().replaceAll("^/+|/+$", "");
            String key = prefix + "/" + userId + "/" + LocalDate.now(ZoneOffset.UTC) + "/" + UUID.randomUUID() + "."
                    + type.extension;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(type.contentType);
            client.putObject(
                    new PutObjectRequest(properties.bucket(), key, new ByteArrayInputStream(content), metadata));
            return new UploadedImageVO(key, properties.domain().replaceAll("/+$", "") + "/" + key);
        } catch (IOException exception) {
            throw ApiException.fileReadFailure();
        } catch (OSSException | ClientException exception) {
            throw ApiException.uploadFailure();
        }
    }

    private enum ImageType {
        PNG("png", "image/png"),
        JPEG("jpg", "image/jpeg"),
        GIF("gif", "image/gif"),
        WEBP("webp", "image/webp");

        private final String extension;
        private final String contentType;

        ImageType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        static ImageType detect(byte[] bytes) {
            if (matches(bytes, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
            if (matches(bytes, 0, 0xFF, 0xD8, 0xFF)) return JPEG;
            if (matches(bytes, 0, 'G', 'I', 'F', '8', '7', 'a') || matches(bytes, 0, 'G', 'I', 'F', '8', '9', 'a'))
                return GIF;
            if (matches(bytes, 0, 'R', 'I', 'F', 'F') && matches(bytes, 8, 'W', 'E', 'B', 'P')) return WEBP;
            return null;
        }

        private static boolean matches(byte[] bytes, int offset, int... signature) {
            if (bytes.length < offset + signature.length) return false;
            for (int i = 0; i < signature.length; i++) {
                if ((bytes[offset + i] & 0xFF) != signature[i]) return false;
            }
            return true;
        }
    }
}
