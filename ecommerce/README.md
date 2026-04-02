# E-Commerce Backend — Spring Boot

A production-grade REST API backend for an e-commerce platform with JWT authentication,
product catalog, cart management, order processing, and Stripe payment integration.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Language | Java 17 |
| Security | Spring Security + JWT (JJWT 0.11) |
| Database | H2 (dev) / MySQL (prod) via Spring Data JPA |
| Payments | Stripe Java SDK |
| Testing | JUnit 5 + Mockito + MockMvc |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/ecommerce/
├── config/
│   ├── SecurityConfig.java        # JWT filter chain, CORS, role guards
│   └── DataLoader.java            # Seeds admin user + sample products on startup
├── controller/
│   ├── AuthController.java        # POST /register, /login
│   ├── ProductController.java     # CRUD + search + pagination
│   ├── CartController.java        # Add / update / remove / clear
│   ├── OrderController.java       # Place order, view orders, cancel, admin ops
│   └── PaymentController.java     # Create PaymentIntent, Stripe webhook
├── dto/                           # Request / response DTOs with Bean Validation
├── entity/
│   ├── User.java                  # ROLE_USER | ROLE_ADMIN
│   ├── Product.java               # Soft-deletable product catalog
│   ├── Cart.java + CartItem.java  # One cart per user, Many-to-Many via join entity
│   ├── Order.java + OrderItem.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── Resource/BadRequest/UnauthorizedException.java
├── repository/                    # Spring Data JPA repositories
├── security/
│   ├── JwtUtils.java              # Token generation & validation
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
├── service/
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── ProductServiceImpl.java
│       ├── CartServiceImpl.java
│       ├── OrderServiceImpl.java
│       └── PaymentServiceImpl.java
└── util/
    └── SecurityUtils.java         # Get current user email from SecurityContext
```

---

## Quick Start

### 1. Clone and configure

```bash
git clone <repo-url>
cd ecommerce-backend
```

Edit `src/main/resources/application.properties`:

```properties
# Add your Stripe keys
stripe.api.key=sk_test_YOUR_KEY
stripe.webhook.secret=whsec_YOUR_SECRET
```

### 2. Run the application

```bash
mvn spring-boot:run
```

The server starts at **http://localhost:8080**

### 3. H2 Console (dev)

Visit **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:ecommercedb`
- Username: `sa` / Password: *(empty)*

### 4. Seeded accounts

| Email | Password | Role |
|---|---|---|
| admin@ecommerce.com | Admin@1234 | ADMIN |
| user@ecommerce.com | User@1234 | USER |

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login → returns JWT |

**Register body:**
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "password": "Password@123"
}
```

**Login body:**
```json
{ "email": "jane@example.com", "password": "Password@123" }
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGci...",
    "type": "Bearer",
    "email": "jane@example.com",
    "role": "ROLE_USER"
  }
}
```

> All protected endpoints require: `Authorization: Bearer <token>`

---

### Products

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | Public | List all (paginated) |
| GET | `/api/products/{id}` | Public | Get by ID |
| GET | `/api/products/category/{cat}` | Public | Filter by category |
| GET | `/api/products/search?keyword=` | Public | Full-text search |
| POST | `/api/products` | ADMIN | Create product |
| PUT | `/api/products/{id}` | ADMIN | Update product |
| DELETE | `/api/products/{id}` | ADMIN | Soft delete |

**Pagination:** `?page=0&size=10&sort=price,asc`

---

### Cart

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/cart` | USER | View cart |
| POST | `/api/cart/items` | USER | Add item |
| PATCH | `/api/cart/items/{id}?quantity=2` | USER | Update quantity |
| DELETE | `/api/cart/items/{id}` | USER | Remove item |
| DELETE | `/api/cart` | USER | Clear cart |

**Add item body:**
```json
{ "productId": 1, "quantity": 2 }
```

---

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | USER | Create order from cart |
| GET | `/api/orders` | USER | My orders (paginated) |
| GET | `/api/orders/{id}` | USER | Order detail |
| PATCH | `/api/orders/{id}/cancel` | USER | Cancel order |
| GET | `/api/orders/admin/all` | ADMIN | All orders |
| PATCH | `/api/orders/admin/{id}/status` | ADMIN | Update status |

**Create order body:**
```json
{
  "shippingName": "Jane Doe",
  "shippingAddress": "123 Main St",
  "shippingCity": "New York",
  "shippingState": "NY",
  "shippingZipCode": "10001",
  "shippingCountry": "US"
}
```

---

### Payments (Stripe)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/payments/create-intent/{orderId}` | USER | Create PaymentIntent |
| POST | `/api/payments/webhook` | Public | Stripe webhook handler |

**Payment flow:**
1. Create order → get `orderId`
2. Call `POST /api/payments/create-intent/{orderId}` → receive `clientSecret`
3. Use **Stripe.js** on frontend to confirm payment with card details
4. Stripe calls `/api/payments/webhook` → order status updated automatically

**Configure Stripe webhook** in dashboard to send:
- `payment_intent.succeeded`
- `payment_intent.payment_failed`

---

## Error Responses

All errors follow a consistent format:

```json
{
  "success": false,
  "message": "Product not found with id: 99",
  "timestamp": "2026-04-02T10:15:30"
}
```

| Status | Scenario |
|---|---|
| 200 OK | Success |
| 201 Created | Resource created |
| 400 Bad Request | Validation failure, bad input |
| 401 Unauthorized | Missing or invalid JWT |
| 403 Forbidden | Insufficient role |
| 404 Not Found | Resource doesn't exist |
| 500 Internal Server Error | Unexpected server error |

---

## Running Tests

```bash
mvn test
```

Test coverage includes:
- `AuthServiceTest` — register/login, duplicate email, bad credentials
- `CartServiceTest` — add/update/remove items, stock validation, ownership checks
- `OrderServiceTest` — order creation, stock deduction, cancellation, payment status updates
- `ProductServiceTest` — CRUD, soft delete, pagination, search
- `AuthControllerTest` — HTTP layer validation via MockMvc

---

## Switching to MySQL (Production)

1. Remove H2 dependency from `pom.xml`, add MySQL connector
2. Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommercedb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

## Security Design

- Passwords hashed with **BCrypt** (strength 12)
- JWT tokens signed with **HMAC-SHA256**, expire in 24 hours
- Role-based access: `ROLE_USER` / `ROLE_ADMIN`
- Stripe webhook signatures verified using HMAC to prevent spoofing
- Cart/order ownership enforced at service layer (users can only access their own data)
- Soft deletes preserve historical order data integrity
