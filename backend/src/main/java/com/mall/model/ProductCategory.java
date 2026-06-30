package com.mall.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_product_category")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 255)
    private String icon;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;
}
