package com.mall.controller;

import com.mall.common.Result;
import com.mall.model.ProductCategory;
import com.mall.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductService productService;

    public CategoryController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<List<ProductCategory>> listCategories(@RequestParam(required = false) Long parentId) {
        return Result.ok(productService.listCategories(parentId));
    }
}
