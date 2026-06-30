package com.mall.controller;

import com.mall.common.CurrentUserHolder;
import com.mall.common.Result;
import com.mall.model.Order;
import com.mall.model.dto.CheckoutRequest;
import com.mall.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public Result<Order> checkout(@Valid @RequestBody CheckoutRequest request) {
        return Result.ok(orderService.checkout(CurrentUserHolder.getUserId(), request));
    }

    @GetMapping
    public Result<Page<Order>> listOrders(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listOrders(CurrentUserHolder.getUserId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        return Result.ok(orderService.getOrder(CurrentUserHolder.getUserId(), id));
    }

    @PostMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.payOrder(CurrentUserHolder.getUserId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(CurrentUserHolder.getUserId(), id);
        return Result.ok();
    }
}
