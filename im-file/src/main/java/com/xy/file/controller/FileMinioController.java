package com.xy.file.controller;


import com.xy.file.entity.OssFile;
import com.xy.file.service.OssFileService;
import com.xy.file.util.ResponseResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * minio上传流程
 * <p>
 * 1.检查数据库中是否存在上传文件
 * <p>
 * 2.根据文件信息初始化，获取分片预签名url地址，前端根据url地址上传文件
 * <p>
 * 3.上传完成后，将分片上传的文件进行合并
 * <p>
 * 4.保存文件信息到数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileMinioController {

    @Resource
    private OssFileService ossFileService;

    /**
     * 校验文件是否存在
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/check")
    public ResponseResult checkFileExistByMd5(@RequestParam("md5") String identifier) {
        log.info("[文件校验] 检查文件是否存在，MD5: {}", identifier);
        return ossFileService.getMultipartUploadProgress(identifier);
    }

    /**
     * 分片初始化
     *
     * @param ossFile 文件信息
     */
    @PostMapping("/multipart/init")
    public ResponseResult initMultiPartUpload(@RequestBody OssFile ossFile) {
        log.info("[分片初始化] 开始初始化分片上传任务，文件信息: {}", ossFile);
        return ossFileService.initMultiPartUpload(ossFile);
    }

    /**
     * 完成上传
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/merge")
    public ResponseResult mergeMultiPartUpload(@RequestParam("md5") String identifier) {
        log.info("[分片合并] 合并分片上传任务，MD5: {}", identifier);
        return ossFileService.mergeMultipartUpload(identifier);
    }

    /**
     * 判断文件是否存在
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/isExits")
    public ResponseResult checkFileExists(@RequestParam("md5") String identifier) {
        log.info("[文件检查] 判断文件是否存在，MD5: {}", identifier);
        return ossFileService.isExits(identifier);
    }

    /**
     * 分片下载
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/download")
    public ResponseEntity downloadFile(@RequestParam("md5") String identifier,
                                       @RequestHeader(value = "Range", required = false) String range) {
        log.info("[文件下载] 开始文件分片下载，MD5: {}, Range: {}", identifier, range);
        return ossFileService.downloadFile(identifier, range);
    }

}

