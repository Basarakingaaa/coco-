package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.model.CartItem;
import com.mall.model.dto.AddCartRequest;
import com.mall.repository.CartItemRepository;
import com.mall.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    @Transactional
    public CartItem addToCart(Long userId, AddCartRequest request) {
        CartItem item = cartItemRepository
                .findByUserIdAndProductIdAndSkuId(userId, request.getProductId(), request.getSkuId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUserId(userId);
                    newItem.setProductId(request.getProductId());
                    newItem.setSkuId(request.getSkuId());
                    newItem.setQuantity(0);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.getQuantity());
        return cartItemRepository.save(item);
    }

    @Override
    public List<CartItem> listCart(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(404, "购物车项不存在"));
        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return;
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItems(Long userId, List<Long> cartItemIds) {
        cartItemRepository.deleteByIdInAndUserId(cartItemIds, userId);
    }
}
