package com.mall.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 商品子信息2：规格参数（键值对，如品牌/产地/重量）
 */
@Data
@Entity
@Table(name = "t_product_info2")
public class ProductInfo2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "spec_key", nullable = false, length = 64)
    private String specKey;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String specValue;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
