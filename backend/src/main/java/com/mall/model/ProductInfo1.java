package com.mall.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 商品子信息1：图文详情（详情页大图、富文本描述）
 */
@Data
@Entity
@Table(name = "t_product_info1")
public class ProductInfo1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Lob
    @Column(name = "detail_html", columnDefinition = "LONGTEXT")
    private String detailHtml;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
