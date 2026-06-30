package com.mall.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryAdminRequest {
    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Long parentId = 0L;
    private Integer sortOrder = 0;
    private String icon;
    private Integer status = 1;
}
