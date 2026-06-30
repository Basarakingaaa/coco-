package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.model.CartItem;
import com.mall.model.MediaType;
import com.mall.model.Order;
import com.mall.model.OrderItem;
import com.mall.model.Product;
import com.mall.model.ProductSku;
import com.mall.model.dto.CheckoutRequest;
import com.mall.repository.CartItemRepository;
import com.mall.repository.OrderItemRepository;
import com.mall.repository.OrderRepository;
import com.mall.repository.ProductMediaRepository;
import com.mall.repository.ProductRepository;
import com.mall.repository.ProductSkuRepository;
import com.mall.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductMediaRepository productMediaRepository;

    public OrderServiceImpl(CartItemRepository cartItemRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             ProductRepository productRepository,
                             ProductSkuRepository productSkuRepository,
                             ProductMediaRepository productMediaRepository) {
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.productSkuRepository = productSkuRepository;
        this.productMediaRepository = productMediaRepository;
    }

    @Override
    @Transactional
    public Order checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByIdInAndUserId(request.getCartItemIds(), userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车项不存在");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setStatus(0);

        BigDecimal total = BigDecimal.ZERO;
        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new BusinessException(404, "商品不存在: " + cartItem.getProductId()));

            BigDecimal price = product.getPrice();
            String image = productMediaRepository
                    .findFirstByProductIdAndMediaTypeOrderBySortOrderAsc(product.getId(), MediaType.IMAGE)
                    .map(m -> m.getUrl())
                    .orElse(null);

            if (cartItem.getSkuId() != null) {
                ProductSku sku = productSkuRepository.findById(cartItem.getSkuId())
                        .orElseThrow(() -> new BusinessException(404, "SKU不存在: " + cartItem.getSkuId()));
                if (sku.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("库存不足: " + product.getName());
                }
                sku.setStock(sku.getStock() - cartItem.getQuantity());
                productSkuRepository.save(sku);
                price = sku.getPrice();
                image = sku.getImage() != null ? sku.getImage() : image;
            } else {
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new BusinessException("库存不足: " + product.getName());
                }
                product.setStock(product.getStock() - cartItem.getQuantity());
                productRepository.save(product);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setSkuId(cartItem.getSkuId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(image);
            orderItem.setPrice(price);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItemRepository.save(orderItem);

            total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        cartItemRepository.deleteByIdInAndUserId(request.getCartItemIds(), userId);
        return order;
    }

    @Override
    public Page<Order> listOrders(Long userId, int page, int size) {
        return orderRepository.findByUserId(userId, PageRequest.of(Math.max(page - 1, 0), size));
    }

    @Override
    public Order getOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    @Override
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = getOrder(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }
        order.setStatus(4);
        orderRepository.save(order);
    }

    private String generateOrderNo() {
        return LocalDateTime.now().toString().replaceAll("[-:.T]", "")
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    public Page<Order> adminListOrders(int page, int size) {
        return orderRepository.findAll(PageRequest.of(Math.max(page - 1, 0), size));
    }

    @Override
    @Transactional
    public Order adminUpdateStatus(Long orderId, Integer status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
