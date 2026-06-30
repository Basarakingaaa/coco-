package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.model.ProductCategory;
import com.mall.model.dto.CategoryAdminRequest;
import com.mall.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final ProductService productService;

    public AdminCategoryController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<List<ProductCategory>> list() {
        return Result.ok(productService.adminListAllCategories());
    }

    @PostMapping
    public Result<ProductCategory> create(@Valid @RequestBody CategoryAdminRequest request) {
        return Result.ok(productService.createCategory(request));
    }

    @PutMapping("/{id}")
    public Result<ProductCategory> update(@PathVariable Long id, @Valid @RequestBody CategoryAdminRequest request) {
        return Result.ok(productService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteCategory(id);
        return Result.ok();
    }
}
