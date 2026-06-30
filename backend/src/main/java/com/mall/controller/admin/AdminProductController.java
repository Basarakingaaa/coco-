package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.model.Product;
import com.mall.model.ProductMedia;
import com.mall.model.dto.ProductAdminRequest;
import com.mall.model.dto.ProductListItem;
import com.mall.model.dto.ProductMediaRequest;
import com.mall.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<Page<ProductListItem>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.adminListProducts(page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @PostMapping
    public Result<Product> create(@Valid @RequestBody ProductAdminRequest request) {
        return Result.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @Valid @RequestBody ProductAdminRequest request) {
        return Result.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.ok();
    }

    // ---- 商品资料（图片/视频），与商品上下架状态解耦 ----

    @GetMapping("/{id}/media")
    public Result<List<ProductMedia>> listMedia(@PathVariable Long id) {
        return Result.ok(productService.listMedia(id));
    }

    @PostMapping("/{id}/media")
    public Result<ProductMedia> addMedia(@PathVariable Long id, @Valid @RequestBody ProductMediaRequest request) {
        return Result.ok(productService.addMedia(id, request));
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public Result<Void> deleteMedia(@PathVariable Long id, @PathVariable Long mediaId) {
        productService.deleteMedia(id, mediaId);
        return Result.ok();
    }
}
