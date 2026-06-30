package com.mall.service;

import com.mall.model.CartItem;
import com.mall.model.dto.AddCartRequest;

import java.util.List;

public interface CartService {
    CartItem addToCart(Long userId, AddCartRequest request);
    List<CartItem> listCart(Long userId);
    void updateQuantity(Long userId, Long cartItemId, Integer quantity);
    void removeItems(Long userId, List<Long> cartItemIds);
}
