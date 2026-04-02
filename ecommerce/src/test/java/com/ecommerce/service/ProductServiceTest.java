package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Laptop X")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(50)
                .category("Electronics")
                .imageUrl("https://example.com/laptop.jpg")
                .active(true)
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Laptop X");
        productRequest.setDescription("High performance laptop");
        productRequest.setPrice(new BigDecimal("999.99"));
        productRequest.setStockQuantity(50);
        productRequest.setCategory("Electronics");
        productRequest.setImageUrl("https://example.com/laptop.jpg");
    }

    @Test
    @DisplayName("createProduct() - persists and returns product response")
    void createProduct_ValidRequest_ReturnsProductResponse() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response.getName()).isEqualTo("Laptop X");
        assertThat(response.getPrice()).isEqualByComparingTo("999.99");
        assertThat(response.getStockQuantity()).isEqualTo(50);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("getProductById() - returns product when found")
    void getProductById_Found_ReturnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop X");
    }

    @Test
    @DisplayName("getProductById() - throws ResourceNotFoundException when not found")
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getAllProducts() - returns paginated active products")
    void getAllProducts_ReturnsPaginatedResults() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findByActiveTrue(any(PageRequest.class))).thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllProducts(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop X");
    }

    @Test
    @DisplayName("updateProduct() - updates all fields and saves")
    void updateProduct_ValidRequest_UpdatesAndReturns() {
        productRequest.setName("Laptop X Pro");
        productRequest.setPrice(new BigDecimal("1199.99"));

        Product updated = Product.builder()
                .id(1L).name("Laptop X Pro")
                .description("High performance laptop")
                .price(new BigDecimal("1199.99"))
                .stockQuantity(50).category("Electronics")
                .imageUrl("https://example.com/laptop.jpg").active(true).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        ProductResponse response = productService.updateProduct(1L, productRequest);

        assertThat(response.getName()).isEqualTo("Laptop X Pro");
        assertThat(response.getPrice()).isEqualByComparingTo("1199.99");
    }

    @Test
    @DisplayName("deleteProduct() - soft deletes by setting active=false")
    void deleteProduct_SetsActiveFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }
}
