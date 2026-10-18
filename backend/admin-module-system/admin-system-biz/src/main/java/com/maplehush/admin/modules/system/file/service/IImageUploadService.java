package com.maplehush.admin.modules.system.file.service;

import com.maplehush.admin.modules.system.file.vo.UploadedImageVO;
import org.springframework.web.multipart.MultipartFile;

/** 图片校验与对象存储上传 */
public interface IImageUploadService {
    /** 图片最大上传大小：5 MiB。 */
    long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    public UploadedImageVO upload(long userId, MultipartFile file);
}
