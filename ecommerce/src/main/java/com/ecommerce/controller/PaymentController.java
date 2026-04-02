package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.PaymentIntentResponse;
import com.ecommerce.service.PaymentService;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/create-intent/{orderId}
     * Create a Stripe PaymentIntent for the given order.
     * Returns clientSecret for the frontend to confirm the payment via Stripe.js.
     *
     * Flow:
     *   1. Client calls this endpoint → gets clientSecret
     *   2. Client uses Stripe.js to confirm payment with card details
     *   3. Stripe sends webhook to /api/payments/webhook on success/failure
     */
    @PostMapping("/create-intent/{orderId}")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @PathVariable Long orderId) {

        String email = SecurityUtils.getCurrentUserEmail();
        PaymentIntentResponse response = paymentService.createPaymentIntent(email, orderId);
        return ResponseEntity.ok(ApiResponse.success(
                "Payment intent created. Use clientSecret to confirm payment.", response));
    }

    /**
     * POST /api/payments/webhook
     * Public endpoint — receives Stripe webhook events.
     * Stripe-Signature header is verified using the webhook secret to prevent spoofing.
     *
     * Configure your Stripe dashboard to send these events:
     *   - payment_intent.succeeded
     *   - payment_intent.payment_failed
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook");
        paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("Webhook processed");
    }
}
