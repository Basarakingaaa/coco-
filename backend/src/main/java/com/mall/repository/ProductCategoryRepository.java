package com.mall.repository;

import com.mall.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByParentIdAndStatus(Long parentId, Integer status);
}
