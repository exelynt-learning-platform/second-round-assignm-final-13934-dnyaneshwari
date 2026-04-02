package com.ecommerce.config;

import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("!test")   // Don't run during unit tests
    public CommandLineRunner seedData() {
        return args -> {
            seedAdminUser();
            seedProducts();
        };
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@ecommerce.com")) {
            User admin = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@ecommerce.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .role(User.Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin user seeded: admin@ecommerce.com / Admin@1234");
        }

        if (!userRepository.existsByEmail("user@ecommerce.com")) {
            User user = User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("user@ecommerce.com")
                    .password(passwordEncoder.encode("User@1234"))
                    .role(User.Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            log.info("Demo user seeded: user@ecommerce.com / User@1234");
        }
    }

    private void seedProducts() {
        if (productRepository.count() > 0) return;

        List<Product> products = List.of(
            Product.builder()
                .name("Apple MacBook Pro 14\"")
                .description("M3 Pro chip, 18GB RAM, 512GB SSD. Exceptional performance for professionals.")
                .price(new BigDecimal("1999.99"))
                .stockQuantity(50)
                .category("Electronics")
                .imageUrl("https://example.com/macbook.jpg")
                .active(true).build(),

            Product.builder()
                .name("Sony WH-1000XM5 Headphones")
                .description("Industry-leading noise cancellation. 30-hour battery life.")
                .price(new BigDecimal("349.99"))
                .stockQuantity(120)
                .category("Electronics")
                .imageUrl("https://example.com/sony-headphones.jpg")
                .active(true).build(),

            Product.builder()
                .name("Samsung 4K OLED TV 55\"")
                .description("4K OLED panel with quantum HDR. Perfect for home cinema.")
                .price(new BigDecimal("1299.99"))
                .stockQuantity(30)
                .category("Electronics")
                .imageUrl("https://example.com/samsung-tv.jpg")
                .active(true).build(),

            Product.builder()
                .name("Nike Air Max 270")
                .description("Lightweight running shoes with MAX Air cushioning. Available in multiple colors.")
                .price(new BigDecimal("129.99"))
                .stockQuantity(200)
                .category("Footwear")
                .imageUrl("https://example.com/nike-airmax.jpg")
                .active(true).build(),

            Product.builder()
                .name("Levi's 501 Original Jeans")
                .description("The original straight-fit jeans. 100% cotton denim.")
                .price(new BigDecimal("59.99"))
                .stockQuantity(300)
                .category("Clothing")
                .imageUrl("https://example.com/levis-501.jpg")
                .active(true).build(),

            Product.builder()
                .name("The Pragmatic Programmer")
                .description("From journeyman to master. A must-read for every developer.")
                .price(new BigDecimal("44.99"))
                .stockQuantity(500)
                .category("Books")
                .imageUrl("https://example.com/pragmatic-programmer.jpg")
                .active(true).build(),

            Product.builder()
                .name("Instant Pot Duo 7-in-1")
                .description("Electric pressure cooker. 7 smart programs. 6-quart capacity.")
                .price(new BigDecimal("89.99"))
                .stockQuantity(80)
                .category("Home & Kitchen")
                .imageUrl("https://example.com/instant-pot.jpg")
                .active(true).build(),

            Product.builder()
                .name("Logitech MX Master 3S Mouse")
                .description("Advanced wireless mouse with ultra-fast MagSpeed scrolling.")
                .price(new BigDecimal("99.99"))
                .stockQuantity(150)
                .category("Electronics")
                .imageUrl("https://example.com/mx-master.jpg")
                .active(true).build()
        );

        productRepository.saveAll(products);
        log.info("Seeded {} sample products.", products.size());
    }
}
