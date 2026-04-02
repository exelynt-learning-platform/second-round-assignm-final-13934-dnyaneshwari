package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.CartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * GET /api/cart
     * Get current user's cart.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(email)));
    }

    /**
     * POST /api/cart/items
     * Add a product to the cart.
     * Body: { "productId": 1, "quantity": 2 }
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemRequest request) {

        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.addItem(email, request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cart));
    }

    /**
     * PATCH /api/cart/items/{cartItemId}?quantity=3
     * Update the quantity of a cart item.
     * Setting quantity to 0 removes the item.
     */
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId,
            @RequestParam @Min(0) int quantity) {

        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.updateItem(email, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart updated", cart));
    }

    /**
     * DELETE /api/cart/items/{cartItemId}
     * Remove a specific item from the cart.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long cartItemId) {

        String email = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.removeItem(email, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    /**
     * DELETE /api/cart
     * Clear all items from the cart.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        String email = SecurityUtils.getCurrentUserEmail();
        cartService.clearCart(email);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
