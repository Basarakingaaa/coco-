package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/webm", "video/quicktime"
    );

    private static final long MAX_SIZE_BYTES = 200L * 1024 * 1024; // 200MB

    private final MinioClient minioClient;

    @Value("${mall.minio.bucket}")
    private String bucket;

    @Value("${mall.minio.public-url}")
    private String publicUrl;

    public FileServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("文件大小不能超过200MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("不支持的文件类型，仅允许图片(jpg/png/webp/gif)或视频(mp4/webm/mov)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String objectName = "uploads/" + UUID.randomUUID() + extension;

        try (var inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }

        return publicUrl + "/" + objectName;
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith(publicUrl + "/")) {
            throw new BusinessException("非法的文件地址");
        }
        String objectName = url.substring((publicUrl + "/").length());
        if (!objectName.startsWith("uploads/") || objectName.contains("..")) {
            throw new BusinessException("非法的文件地址");
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(500, "文件删除失败: " + e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(dotIndex).toLowerCase();
        List<String> safeExtensions = List.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".mp4", ".webm", ".mov");
        return safeExtensions.contains(ext) ? ext : "";
    }
}
