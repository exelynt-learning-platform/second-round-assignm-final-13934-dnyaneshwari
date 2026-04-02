package com.ecommerce.service;

import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartServiceImpl cartService;

    private User user;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("user@test.com")
                .firstName("Test").lastName("User")
                .role(User.Role.ROLE_USER).build();

        product = Product.builder()
                .id(10L).name("Test Product")
                .price(new BigDecimal("99.99"))
                .stockQuantity(20)
                .active(true).build();

        cart = Cart.builder()
                .id(1L).user(user)
                .items(new ArrayList<>()).build();
    }

    @Test
    @DisplayName("addItem() - adds new item to empty cart")
    void addItem_NewProduct_AddsToCart() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(10L);
        request.setQuantity(2);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItem("user@test.com", request);

        assertThat(response).isNotNull();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("addItem() - throws when product is out of stock")
    void addItem_InsufficientStock_ThrowsBadRequest() {
        product.setStockQuantity(1);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(10L);
        request.setQuantity(5);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem("user@test.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("addItem() - throws when product is inactive")
    void addItem_InactiveProduct_ThrowsBadRequest() {
        product.setActive(false);

        CartItemRequest request = new CartItemRequest();
        request.setProductId(10L);
        request.setQuantity(1);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem("user@test.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    @DisplayName("addItem() - creates new cart if user has none")
    void addItem_NoExistingCart_CreatesNewCart() {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(10L);
        request.setQuantity(1);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartAndProduct(any(), any())).thenReturn(Optional.empty());

        cartService.addItem("user@test.com", request);

        // Cart must have been created (saved twice: once for creation, once after adding item)
        verify(cartRepository, atLeast(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("removeItem() - removes existing cart item")
    void removeItem_ExistingItem_RemovesFromCart() {
        CartItem cartItem = CartItem.builder()
                .id(5L).cart(cart).product(product)
                .quantity(2).priceAtAddition(product.getPrice()).build();
        cart.getItems().add(cartItem);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.removeItem("user@test.com", 5L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("removeItem() - throws when cart item not found")
    void removeItem_NonExistentItem_ThrowsNotFound() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem("user@test.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("clearCart() - empties all cart items")
    void clearCart_ClearsAllItems() {
        CartItem item = CartItem.builder().id(1L).cart(cart).product(product)
                .quantity(1).priceAtAddition(product.getPrice()).build();
        cart.getItems().add(item);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenReturn(cart);

        cartService.clearCart("user@test.com");

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }
}
