package com.mall.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_ad_category")
public class AdCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer status = 1;
}
