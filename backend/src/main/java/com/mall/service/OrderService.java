package com.mall.service;

import com.mall.model.Order;
import com.mall.model.dto.CheckoutRequest;
import org.springframework.data.domain.Page;

public interface OrderService {
    Order checkout(Long userId, CheckoutRequest request);
    Page<Order> listOrders(Long userId, int page, int size);
    Order getOrder(Long userId, Long orderId);
    void payOrder(Long userId, Long orderId);
    void cancelOrder(Long userId, Long orderId);

    // ---- 管理端 ----
    Page<Order> adminListOrders(int page, int size);
    Order adminUpdateStatus(Long orderId, Integer status);
}
