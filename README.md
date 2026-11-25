# Hostel & PG Booking Platform Backend

Production-oriented Spring Boot backend for short-term (hourly/nightly) and long-term (weekly/monthly) hostel and PG accommodation bookings in India.

## Technology Stack
- **Java 21** & **Spring Boot 3.4.3** (Spring MVC, Spring Data JPA, Spring Security, Bean Validation, Actuator)
- **PostgreSQL 16** with **Flyway** migrations and `btree_gist` exclusion constraints
- **JWT Authentication** (`jjwt`) with BCrypt password hashing, refresh token rotation, and auth rate limiting
- **OpenAPI / Swagger UI** (`springdoc-openapi`)
- **JUnit 5, Mockito, Testcontainers (PostgreSQL)**
- **Docker & Docker Compose**

## Architecture
Modular layered architecture under `src/main/java/com/accommodation/platform/`:
- `config/` — Security, OpenAPI, and typed `@ConfigurationProperties`
- `security/` — `JwtService`, `JwtAuthenticationFilter`, `RateLimitFilter`, `UserPrincipal`, `SecurityUtils`
- `common/` — `ApiResponse`, `PagedResponse`, custom exceptions, `GlobalExceptionHandler`, enums, utilities
- `auth/` & `user/` — Registration, login, refresh token rotation, password reset, email verification, profile management
- `property/`, `room/`, `bed/`, `availability/`, `verification/` — Property listings, owner/property KYC verification, pricing, room/bed inventory, availability blocks, search, Emergency Stay, and Interview Mode
- `booking/`, `checkin/`, `coupon/` — Concurrency-safe short/long-stay booking engine, modifications, extensions, walk-ins, check-in/out, booking failures & alternatives, coupons
- `payment/`, `rent/`, `deposit/`, `payout/` — `PaymentGateway` abstraction (`MockPaymentGateway`, `RazorpayPaymentGateway`), idempotent initiation, webhooks, refunds, monthly rent schedules, security deposits, owner commissions & payouts
- `review/`, `favorite/`, `notification/`, `support/`, `dispute/`, `safety/`, `audit/`, `dashboard/`, `analytics/` — Reviews, favorites, notifications, support tickets, disputes, safety complaints, audit logs, owner & admin dashboards, analytics

## Concurrency-Safe Double-Booking Prevention
1. **PostgreSQL `btree_gist` Exclusion Constraint** (`V4__create_bookings.sql`):
   ```sql
   EXCLUDE USING GIST (
       bed_id WITH =,
       tstzrange(check_in, check_out, '[)') WITH &&
   ) WHERE (booking_status IN ('PENDING', 'CONFIRMED'))
   ```
   Half-open `[check_in, check_out)` ranges allow back-to-back bookings while guaranteeing at the database level that two active bookings for the same bed cannot overlap.
2. **Pessimistic Row Locking** (`SELECT ... FOR UPDATE` via `BedRepository.findByIdForUpdate`) serializes concurrent booking transactions on the same bed.
3. **Hold Expiration** (`BookingExpirationScheduler`) automatically expires unpaid/unapproved `PENDING` bookings so inventory is never blocked indefinitely.

## Environment Variables
Copy `.env.example` to `.env` and configure values. Required variables:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` (minimum 32 bytes)
- `FRONTEND_ORIGIN`
- `PAYMENT_PROVIDER` (`MOCK` or `RAZORPAY`), `PAYMENT_WEBHOOK_SECRET`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`

## Running Locally with Docker Compose
```bash
docker compose up --build
```
- API Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health Check: `http://localhost:8080/actuator/health`

## Maven Commands
```bash
# Compile
mvn compile

# Run tests (Testcontainers integration & concurrency tests run automatically when Docker is available)
mvn test

# Package JAR
mvn package -DskipTests
```

## Postman Collection
Import `postman_collection.json` into Postman to test authentication, property creation, search, booking, and payment flows.
