package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrderFromCart(String email, OrderRequest request);
    OrderResponse getOrderById(String email, Long orderId);
    Page<OrderResponse> getUserOrders(String email, Pageable pageable);
    OrderResponse cancelOrder(String email, Long orderId);

    // Called internally by PaymentService on webhook events
    OrderResponse markOrderPaid(Long orderId, String paymentIntentId);
    OrderResponse markOrderPaymentFailed(Long orderId);

    // Admin
    Page<OrderResponse> getAllOrders(Pageable pageable);
    OrderResponse updateOrderStatus(Long orderId, String status);
}
