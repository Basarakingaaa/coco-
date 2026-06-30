package com.mall.repository;

import com.mall.model.MediaType;
import com.mall.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
    List<ProductMedia> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ProductMedia> findFirstByProductIdAndMediaTypeOrderBySortOrderAsc(Long productId, MediaType mediaType);

    List<ProductMedia> findByProductIdInAndMediaTypeOrderBySortOrderAsc(List<Long> productIds, MediaType mediaType);

    List<ProductMedia> findByProductIdInOrderBySortOrderAsc(List<Long> productIds);

    boolean existsByProductId(Long productId);

    void deleteByProductId(Long productId);
}
