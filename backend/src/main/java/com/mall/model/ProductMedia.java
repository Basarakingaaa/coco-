package com.mall.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品资料（图片/视频），与商品本体解耦：一个商品可以有 0~N 条资料，
 * 资料的增删不影响商品的上架状态，反之商品上/下架也不影响已上传的资料。
 */
@Data
@Entity
@Table(name = "t_product_media")
public class ProductMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private MediaType mediaType;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
