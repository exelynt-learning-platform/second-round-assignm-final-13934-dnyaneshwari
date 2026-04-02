package com.ecommerce.service.impl;

import com.ecommerce.dto.PaymentIntentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UnauthorizedException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Override
    public PaymentIntentResponse createPaymentIntent(String email, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Ensure the authenticated user owns this order
        if (!order.getUser().getEmail().equals(email)) {
            throw new UnauthorizedException("You do not have access to this order.");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("This order has already been paid.");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order.");
        }

        try {
            // Stripe amounts are in the smallest currency unit (cents for USD)
            long amountInCents = order.getTotalPrice()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderId", orderId.toString());
            metadata.put("userEmail", email);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .setDescription("Order #" + orderId)
                    .putAllMetadata(metadata)
                    // Automatically confirm when frontend provides payment method
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            log.info("PaymentIntent {} created for Order #{}", intent.getId(), orderId);

            return PaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .paymentIntentId(intent.getId())
                    .amount(order.getTotalPrice())
                    .currency("usd")
                    .orderId(orderId)
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error creating PaymentIntent for Order #{}: {}", orderId, e.getMessage());
            throw new BadRequestException("Payment processing failed: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid webhook signature.");
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> stripeObjectOptional = deserializer.getObject();

        if (stripeObjectOptional.isEmpty()) {
            log.warn("Could not deserialize Stripe event object for type: {}", event.getType());
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) stripeObjectOptional.get();
                handlePaymentSuccess(intent);
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) stripeObjectOptional.get();
                handlePaymentFailure(intent);
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentSuccess(PaymentIntent intent) {
        String orderIdStr = intent.getMetadata().get("orderId");
        if (orderIdStr == null) {
            log.error("No orderId in PaymentIntent metadata: {}", intent.getId());
            return;
        }

        Long orderId = Long.parseLong(orderIdStr);
        orderService.markOrderPaid(orderId, intent.getId());
        log.info("Payment succeeded for Order #{} via PaymentIntent {}", orderId, intent.getId());
    }

    private void handlePaymentFailure(PaymentIntent intent) {
        String orderIdStr = intent.getMetadata().get("orderId");
        if (orderIdStr == null) {
            log.error("No orderId in PaymentIntent metadata: {}", intent.getId());
            return;
        }

        Long orderId = Long.parseLong(orderIdStr);
        orderService.markOrderPaymentFailed(orderId);
        log.warn("Payment failed for Order #{} via PaymentIntent {}", orderId, intent.getId());
    }
}
