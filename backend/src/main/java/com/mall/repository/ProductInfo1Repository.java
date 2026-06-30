package com.mall.repository;

import com.mall.model.ProductInfo1;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductInfo1Repository extends JpaRepository<ProductInfo1, Long> {
    List<ProductInfo1> findByProductIdOrderBySortOrderAsc(Long productId);
}
