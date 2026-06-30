package com.mall.model.dto;

import com.mall.model.Product;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表项：商品本体字段 + 解耦计算出的封面图（来自 t_product_media，可能不存在）。
 * hasMedia 标识该商品当前是否有任何资料（图片/视频），与 status（是否上架）互相独立。
 */
@Data
public class ProductListItem {
    private Long id;
    private Long categoryId;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private String coverImage;
    private boolean hasMedia;

    public static ProductListItem from(Product product, String coverImage, boolean hasMedia) {
        ProductListItem item = new ProductListItem();
        item.setId(product.getId());
        item.setCategoryId(product.getCategoryId());
        item.setName(product.getName());
        item.setSubtitle(product.getSubtitle());
        item.setPrice(product.getPrice());
        item.setStock(product.getStock());
        item.setSales(product.getSales());
        item.setStatus(product.getStatus());
        item.setCoverImage(coverImage);
        item.setHasMedia(hasMedia);
        return item;
    }
}
