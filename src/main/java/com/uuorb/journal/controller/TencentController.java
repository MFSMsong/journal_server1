package com.uuorb.journal.controller;

import com.uuorb.journal.annotation.Authorization;
import com.uuorb.journal.annotation.UserId;
import com.uuorb.journal.controller.vo.CosCredential;
import com.uuorb.journal.controller.vo.Result;
import com.uuorb.journal.controller.vo.UploadCredential;
import com.uuorb.journal.service.CosService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tencent")
public class TencentController {


    @Resource
    private CosService cosService;

    @Authorization
    @GetMapping("/cos/upload-credential")
    public Result<UploadCredential> getUploadCredential(
            @UserId String userId,
            @RequestParam("type") String type,
            @RequestParam("ext") String ext) {
        UploadCredential credential = cosService.generateUploadCredential(type, userId, ext);
        log.info("获取上传凭证: userId={}, type={}, cosPath={}", userId, type, credential.getCosPath());
        return Result.ok(credential);
    }

    @Authorization
    @GetMapping("/cos/presigned-url")
    public Result<String> getPresignedUrl(@RequestParam("cosPath") String cosPath) {
        String url = cosService.generatePresignedUrl(cosPath);
        return Result.ok(url);
    }
}
