package com.ecommerce.service;

import com.ecommerce.dto.PaymentIntentResponse;

public interface PaymentService {
    /**
     * Creates a Stripe PaymentIntent for a given order.
     * Returns the clientSecret which the frontend uses to confirm payment.
     */
    PaymentIntentResponse createPaymentIntent(String email, Long orderId);

    /**
     * Handles incoming Stripe webhook events (payment_intent.succeeded, etc.).
     * Stripe sends a raw payload + a signature header for verification.
     */
    void handleWebhook(String payload, String sigHeader);
}
