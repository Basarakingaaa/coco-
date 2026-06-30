package com.mall.service;

import com.mall.model.Product;
import com.mall.model.ProductCategory;
import com.mall.model.ProductMedia;
import com.mall.model.dto.CategoryAdminRequest;
import com.mall.model.dto.ProductAdminRequest;
import com.mall.model.dto.ProductListItem;
import com.mall.model.dto.ProductMediaRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Page<ProductListItem> listProducts(Long categoryId, String keyword, int page, int size);
    Map<String, Object> getProductDetail(Long productId);
    List<ProductCategory> listCategories(Long parentId);

    // ---- 管理端：商品 ----
    Page<ProductListItem> adminListProducts(int page, int size);
    Product createProduct(ProductAdminRequest request);
    Product updateProduct(Long id, ProductAdminRequest request);
    void deleteProduct(Long id);

    // ---- 管理端：商品资料（图片/视频），与商品本体解耦 ----
    List<ProductMedia> listMedia(Long productId);
    ProductMedia addMedia(Long productId, ProductMediaRequest request);
    void deleteMedia(Long productId, Long mediaId);

    // ---- 管理端：分类 ----
    List<ProductCategory> adminListAllCategories();
    ProductCategory createCategory(CategoryAdminRequest request);
    ProductCategory updateCategory(Long id, CategoryAdminRequest request);
    void deleteCategory(Long id);
}
