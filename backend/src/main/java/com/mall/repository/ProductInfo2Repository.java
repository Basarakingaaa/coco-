package com.mall.repository;

import com.mall.model.ProductInfo2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductInfo2Repository extends JpaRepository<ProductInfo2, Long> {
    List<ProductInfo2> findByProductIdOrderBySortOrderAsc(Long productId);
}
