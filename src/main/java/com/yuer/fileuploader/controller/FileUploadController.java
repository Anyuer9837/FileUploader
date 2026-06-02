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

        // 1. 获取原始文件名并清理路径（主要为了安全获取后缀）
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        // 2. 提取文件后缀名 (例如: .jpg, .png, .pdf)
        String fileExtension = "";
        int lastIndexOf = originalFilename.lastIndexOf(".");
        if (lastIndexOf != -1) {
            fileExtension = originalFilename.substring(lastIndexOf);
        }

        // 3. 生成新文件名：UUID + 时间戳 + 后缀名
        String uuid = UUID.randomUUID().toString().replace("-", ""); // 去掉UUID的横杠，让文件名更整洁
        long timestamp = System.currentTimeMillis();
        String fileName = uuid + "-" + timestamp + fileExtension;

        try {
            // 4. 检查路径非法序列（虽然新生成的文件名理论上很安全，但保留作为兜底）
            if (fileName.contains("..")) {
                response.put("error", "文件名包含非法路径序列");
                return ResponseEntity.badRequest().body(response);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 5. 不再生成绝对 URL，只生成相对路径
            String fileRelativePath = "/files/" + fileName;

            response.put("message", "上传成功");
            response.put("url", fileRelativePath); // 返回给前端：/files/abc_123.jpg
            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            response.put("error", "文件存储失败: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}