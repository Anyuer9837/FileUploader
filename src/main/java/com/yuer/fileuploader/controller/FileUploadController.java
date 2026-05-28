package com.yuer.fileuploader.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
public class FileUploadController {

    private final Path fileStorageLocation;

    public FileUploadController(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            // 自动创建 uploads 文件夹
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "上传文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // 使用 UUID 拼接文件名，防止同名文件覆盖
        String fileName = UUID.randomUUID() + "_" + originalFilename;

        try {
            if (fileName.contains("..")) {
                response.put("error", "文件名包含非法路径序列");
                return ResponseEntity.badRequest().body(response);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 动态生成用户直连的 URL
            String fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/")
                    .path(fileName)
                    .toUriString();

            response.put("message", "上传成功");
            response.put("url", fileDownloadUrl);
            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            response.put("error", "文件存储失败: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}