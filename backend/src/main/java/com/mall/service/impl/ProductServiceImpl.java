package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.model.MediaType;
import com.mall.model.Product;
import com.mall.model.ProductCategory;
import com.mall.model.ProductMedia;
import com.mall.model.dto.CategoryAdminRequest;
import com.mall.model.dto.ProductAdminRequest;
import com.mall.model.dto.ProductListItem;
import com.mall.model.dto.ProductMediaRequest;
import com.mall.repository.ProductCategoryRepository;
import com.mall.repository.ProductInfo1Repository;
import com.mall.repository.ProductInfo2Repository;
import com.mall.repository.ProductMediaRepository;
import com.mall.repository.ProductRepository;
import com.mall.repository.ProductSkuRepository;
import com.mall.service.FileService;
import com.mall.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String DETAIL_CACHE_KEY = "product:detail:";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductInfo1Repository productInfo1Repository;
    private final ProductInfo2Repository productInfo2Repository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMediaRepository productMediaRepository;
    private final FileService fileService;
    private final RedisTemplate<String, Object> redisTemplate;

    public ProductServiceImpl(ProductRepository productRepository,
                               ProductCategoryRepository categoryRepository,
                               ProductInfo1Repository productInfo1Repository,
                               ProductInfo2Repository productInfo2Repository,
                               ProductSkuRepository productSkuRepository,
                               ProductMediaRepository productMediaRepository,
                               FileService fileService,
                               RedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productInfo1Repository = productInfo1Repository;
        this.productInfo2Repository = productInfo2Repository;
        this.productSkuRepository = productSkuRepository;
        this.productMediaRepository = productMediaRepository;
        this.fileService = fileService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Page<ProductListItem> listProducts(Long categoryId, String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);
        Page<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndStatus(categoryId, 1, pageRequest);
        } else if (StringUtils.hasText(keyword)) {
            products = productRepository.findByStatusAndNameContaining(1, keyword, pageRequest);
        } else {
            products = productRepository.findByStatus(1, pageRequest);
        }
        return withCoverImages(products);
    }

    @Override
    public Page<ProductListItem> adminListProducts(int page, int size) {
        Page<Product> products = productRepository.findAll(PageRequest.of(Math.max(page - 1, 0), size));
        return withCoverImages(products);
    }

    private Page<ProductListItem> withCoverImages(Page<Product> products) {
        List<Long> productIds = products.getContent().stream().map(Product::getId).toList();
        Map<Long, ProductMedia> coverByProduct = new LinkedHashMap<>();
        Map<Long, Boolean> hasMediaByProduct = new HashMap<>();

        for (ProductMedia media : productMediaRepository.findByProductIdInOrderBySortOrderAsc(productIds)) {
            hasMediaByProduct.put(media.getProductId(), true);
            if (media.getMediaType() == MediaType.IMAGE) {
                coverByProduct.putIfAbsent(media.getProductId(), media);
            }
        }

        return products.map(product -> ProductListItem.from(
                product,
                coverByProduct.containsKey(product.getId()) ? coverByProduct.get(product.getId()).getUrl() : null,
                hasMediaByProduct.getOrDefault(product.getId(), false)
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getProductDetail(Long productId) {
        String cacheKey = DETAIL_CACHE_KEY + productId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (Map<String, Object>) cached;
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));

        Map<String, Object> detail = new HashMap<>();
        detail.put("product", product);
        detail.put("media", productMediaRepository.findByProductIdOrderBySortOrderAsc(productId));
        detail.put("info1", productInfo1Repository.findByProductIdOrderBySortOrderAsc(productId));
        detail.put("info2", productInfo2Repository.findByProductIdOrderBySortOrderAsc(productId));
        detail.put("skuList", productSkuRepository.findByProductIdAndStatus(productId, 1));

        redisTemplate.opsForValue().set(cacheKey, detail, Duration.ofMinutes(10));
        return detail;
    }

    @Override
    public List<ProductCategory> listCategories(Long parentId) {
        return categoryRepository.findByParentIdAndStatus(parentId == null ? 0L : parentId, 1);
    }

    @Override
    public Product createProduct(ProductAdminRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, ProductAdminRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        applyRequest(product, request);
        product = productRepository.save(product);
        redisTemplate.delete(DETAIL_CACHE_KEY + id);
        return product;
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(404, "商品不存在");
        }
        for (ProductMedia media : productMediaRepository.findByProductIdOrderBySortOrderAsc(id)) {
            tryDeleteFile(media.getUrl());
        }
        productMediaRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        redisTemplate.delete(DETAIL_CACHE_KEY + id);
    }

    private void applyRequest(Product product, ProductAdminRequest request) {
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setStatus(request.getStatus());
    }

    @Override
    public List<ProductMedia> listMedia(Long productId) {
        return productMediaRepository.findByProductIdOrderBySortOrderAsc(productId);
    }

    @Override
    public ProductMedia addMedia(Long productId, ProductMediaRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(404, "商品不存在");
        }
        List<ProductMedia> existing = productMediaRepository.findByProductIdOrderBySortOrderAsc(productId);
        int nextSortOrder = existing.stream().mapToInt(ProductMedia::getSortOrder).max().orElse(-1) + 1;

        ProductMedia media = new ProductMedia();
        media.setProductId(productId);
        media.setMediaType(request.getMediaType());
        media.setUrl(request.getUrl());
        media.setSortOrder(nextSortOrder);
        media = productMediaRepository.save(media);

        redisTemplate.delete(DETAIL_CACHE_KEY + productId);
        return media;
    }

    @Override
    public void deleteMedia(Long productId, Long mediaId) {
        ProductMedia media = productMediaRepository.findById(mediaId)
                .filter(m -> m.getProductId().equals(productId))
                .orElseThrow(() -> new BusinessException(404, "资料不存在"));
        productMediaRepository.delete(media);
        tryDeleteFile(media.getUrl());
        redisTemplate.delete(DETAIL_CACHE_KEY + productId);
    }

    private void tryDeleteFile(String url) {
        try {
            fileService.delete(url);
        } catch (Exception ignored) {
            // 种子数据等非本系统 MinIO 存储的 URL 会被拒绝，忽略即可
        }
    }

    @Override
    public List<ProductCategory> adminListAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public ProductCategory createCategory(CategoryAdminRequest request) {
        ProductCategory category = new ProductCategory();
        applyCategoryRequest(category, request);
        return categoryRepository.save(category);
    }

    @Override
    public ProductCategory updateCategory(Long id, CategoryAdminRequest request) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));
        applyCategoryRequest(category, request);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(404, "分类不存在");
        }
        categoryRepository.deleteById(id);
    }

    private void applyCategoryRequest(ProductCategory category, CategoryAdminRequest request) {
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());
        category.setIcon(request.getIcon());
        category.setStatus(request.getStatus());
    }
}
