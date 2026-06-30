package com.mall.model.dto;

import com.mall.model.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductMediaRequest {
    @NotNull(message = "资料类型不能为空")
    private MediaType mediaType;

    @NotBlank(message = "资料地址不能为空")
    private String url;
}
