package com.mall.repository;

import com.mall.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndProductIdAndSkuId(Long userId, Long productId, Long skuId);
    List<CartItem> findByIdInAndUserId(List<Long> ids, Long userId);
    void deleteByIdInAndUserId(List<Long> ids, Long userId);
}
