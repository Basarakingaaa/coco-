package com.mall.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdAdminRequest {
    @NotNull(message = "广告类别不能为空")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "图片不能为空")
    private String imageUrl;

    private String linkUrl;
    private Integer sortOrder = 0;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status = 1;
}
