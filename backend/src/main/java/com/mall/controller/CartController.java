package com.mall.controller;

import com.mall.common.CurrentUserHolder;
import com.mall.common.Result;
import com.mall.model.CartItem;
import com.mall.model.dto.AddCartRequest;
import com.mall.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<List<CartItem>> listCart() {
        return Result.ok(cartService.listCart(CurrentUserHolder.getUserId()));
    }

    @PostMapping
    public Result<CartItem> addToCart(@Valid @RequestBody AddCartRequest request) {
        return Result.ok(cartService.addToCart(CurrentUserHolder.getUserId(), request));
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        cartService.updateQuantity(CurrentUserHolder.getUserId(), id, body.get("quantity"));
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> removeItems(@RequestBody List<Long> cartItemIds) {
        cartService.removeItems(CurrentUserHolder.getUserId(), cartItemIds);
        return Result.ok();
    }
}
