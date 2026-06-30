package com.mall.repository;

import com.mall.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);
    Page<Product> findByStatusAndNameContaining(Integer status, String name, Pageable pageable);
    Page<Product> findByStatus(Integer status, Pageable pageable);
}
