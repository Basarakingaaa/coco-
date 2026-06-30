package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.model.Order;
import com.mall.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Result<Page<Order>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.adminListOrders(page, size));
    }

    @PutMapping("/{id}/status")
    public Result<Order> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return Result.ok(orderService.adminUpdateStatus(id, body.get("status")));
    }
}
