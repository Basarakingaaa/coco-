package com.mall.repository;

import com.mall.model.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByProductIdAndStatus(Long productId, Integer status);
    Optional<ProductSku> findBySkuCode(String skuCode);
}
