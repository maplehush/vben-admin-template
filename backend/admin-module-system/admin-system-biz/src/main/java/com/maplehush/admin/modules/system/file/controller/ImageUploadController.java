package com.maplehush.admin.modules.system.file.controller;

import com.maplehush.admin.common.api.vo.ApiResult;
import com.maplehush.admin.modules.system.auth.service.ICurrentUserService.CurrentUser;
import com.maplehush.admin.modules.system.file.service.IImageUploadService;
import com.maplehush.admin.modules.system.file.vo.UploadedImageVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageUploadController {
    private final IImageUploadService uploads;

    public ImageUploadController(IImageUploadService uploads) {
        this.uploads = uploads;
    }

    @PostMapping("/upload")
    public ApiResult<UploadedImageVO> upload(
            @AuthenticationPrincipal CurrentUser user, @RequestParam("file") MultipartFile file) {
        return ApiResult.ok(uploads.upload(user.userId(), file));
    }
}
