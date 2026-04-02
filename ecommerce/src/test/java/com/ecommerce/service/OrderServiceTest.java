package com.ecommerce.service;

import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("user@test.com")
                .firstName("John").lastName("Doe")
                .role(User.Role.ROLE_USER).build();

        product = Product.builder()
                .id(10L).name("Widget Pro")
                .price(new BigDecimal("49.99"))
                .stockQuantity(100)
                .active(true).build();

        cartItem = CartItem.builder()
                .id(1L).product(product)
                .quantity(3)
                .priceAtAddition(new BigDecimal("49.99")).build();

        cart = Cart.builder()
                .id(1L).user(user)
                .items(new ArrayList<>(List.of(cartItem))).build();
        cartItem.setCart(cart);

        orderRequest = new OrderRequest();
        orderRequest.setShippingName("John Doe");
        orderRequest.setShippingAddress("123 Main St");
        orderRequest.setShippingCity("Springfield");
        orderRequest.setShippingState("IL");
        orderRequest.setShippingZipCode("62701");
        orderRequest.setShippingCountry("US");
    }

    @Test
    @DisplayName("createOrderFromCart() - creates order and clears cart")
    void createOrder_ValidCart_CreatesOrderAndClearsCart() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Order createdOrder = Order.builder()
                .id(1L).user(user)
                .totalPrice(new BigDecimal("149.97"))
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .shippingName("John Doe")
                .shippingAddress("123 Main St")
                .shippingCity("Springfield")
                .shippingState("IL")
                .shippingZipCode("62701")
                .shippingCountry("US")
                .items(new ArrayList<>())
                .build();
        when(orderRepository.save(any(Order.class))).thenReturn(createdOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderResponse response = orderService.createOrderFromCart("user@test.com", orderRequest);

        assertThat(response).isNotNull();
        assertThat(response.getTotalPrice()).isEqualByComparingTo("149.97");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPaymentStatus()).isEqualTo("UNPAID");

        // Stock should be decremented
        assertThat(product.getStockQuantity()).isEqualTo(97);

        // Cart should be cleared
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("createOrderFromCart() - throws when cart is empty")
    void createOrder_EmptyCart_ThrowsBadRequest() {
        cart.getItems().clear();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrderFromCart("user@test.com", orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("createOrderFromCart() - throws when product stock is insufficient")
    void createOrder_InsufficientStock_ThrowsBadRequest() {
        product.setStockQuantity(1); // only 1 in stock, but cart has quantity 3

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrderFromCart("user@test.com", orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("cancelOrder() - cancels pending order and restores stock")
    void cancelOrder_PendingOrder_CancelsAndRestoresStock() {
        Order order = Order.builder()
                .id(1L).user(user)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .totalPrice(new BigDecimal("149.97"))
                .shippingName("John Doe").shippingAddress("123 Main St")
                .shippingCity("Springfield").shippingState("IL")
                .shippingZipCode("62701").shippingCountry("US")
                .items(new ArrayList<>())
                .build();

        OrderItem orderItem = OrderItem.builder()
                .id(1L).order(order).product(product)
                .quantity(3).priceAtPurchase(new BigDecimal("49.99")).build();
        order.getItems().add(orderItem);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        int stockBefore = product.getStockQuantity();
        OrderResponse response = orderService.cancelOrder("user@test.com", 1L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(product.getStockQuantity()).isEqualTo(stockBefore + 3); // stock restored
    }

    @Test
    @DisplayName("cancelOrder() - throws when order is already shipped")
    void cancelOrder_ShippedOrder_ThrowsBadRequest() {
        Order order = Order.builder()
                .id(1L).user(user)
                .status(Order.OrderStatus.SHIPPED)
                .items(new ArrayList<>()).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("user@test.com", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("shipped");
    }

    @Test
    @DisplayName("markOrderPaid() - updates payment and order status")
    void markOrderPaid_UpdatesStatuses() {
        Order order = Order.builder()
                .id(1L).user(user)
                .status(Order.OrderStatus.PENDING)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .totalPrice(new BigDecimal("49.99"))
                .shippingName("John Doe").shippingAddress("123 Main St")
                .shippingCity("Springfield").shippingState("IL")
                .shippingZipCode("62701").shippingCountry("US")
                .items(new ArrayList<>()).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.markOrderPaid(1L, "pi_test_123");

        assertThat(response.getPaymentStatus()).isEqualTo("PAID");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getPaymentIntentId()).isEqualTo("pi_test_123");
    }

    @Test
    @DisplayName("getOrderById() - throws when order belongs to another user")
    void getOrderById_WrongUser_ThrowsNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("user@test.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
