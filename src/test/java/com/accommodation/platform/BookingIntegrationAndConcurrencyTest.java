package com.accommodation.platform;

import com.accommodation.platform.auth.dto.AuthDtos.RegisterRequest;
import com.accommodation.platform.booking.dto.BookingDtos.CreateBookingRequest;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.dashboard.service.AdminDashboardService;
import com.accommodation.platform.payment.dto.PaymentDtos.InitiatePaymentRequest;
import com.accommodation.platform.payment.dto.PaymentDtos.VerifyPaymentRequest;
import com.accommodation.platform.payment.dto.PaymentDtos.WebhookPayload;
import com.accommodation.platform.payment.service.PaymentService;
import com.accommodation.platform.property.dto.PropertyDtos.CreatePropertyRequest;
import com.accommodation.platform.property.dto.PropertyDtos.PricingRequest;
import com.accommodation.platform.property.service.PropertySearchService;
import com.accommodation.platform.property.service.PropertyService;
import com.accommodation.platform.room.dto.RoomDtos.CreateRoomRequest;
import com.accommodation.platform.room.service.RoomService;
import com.accommodation.platform.auth.service.AuthService;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BookingIntegrationAndConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hostel_booking_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-key-at-least-32-bytes");
    }

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyService propertyService;
    @Autowired private RoomService roomService;
    @Autowired private AdminDashboardService adminService;
    @Autowired private PropertySearchService searchService;
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;

    @Test
    void concurrentOverlappingBookingsAllowOnlyOneWinnerAndAllowBackToBack() throws Exception {
        var ownerAuth = authService.register(new RegisterRequest(
                "Owner One", "owner1@example.com", "Strong@123", "9876543210", UserRole.PROPERTY_OWNER, Gender.MALE
        ));
        var user1Auth = authService.register(new RegisterRequest(
                "Student One", "student1@example.com", "Strong@123", "9876543211", UserRole.CUSTOMER, Gender.MALE
        ));
        var user2Auth = authService.register(new RegisterRequest(
                "Student Two", "student2@example.com", "Strong@123", "9876543212", UserRole.CUSTOMER, Gender.MALE
        ));

        User admin = new User();
        admin.setFullName("Admin");
        admin.setEmail("admin@example.com");
        admin.setPasswordHash("hashed");
        admin.setPhoneNumber("9876543299");
        admin.setRole(UserRole.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin = userRepository.save(admin);

        var prop = propertyService.createProperty(ownerAuth.user().id(), new CreatePropertyRequest(
                "Metro Boys Hostel", "Near IT Park", PropertyType.HOSTEL,
                "12 MG Road", "Indiranagar", "Bengaluru", "Karnataka", "560038",
                12.9716, 77.5946, GenderPolicy.MALE_ONLY, null, null, true,
                3, 14, 7, 12, 1, Set.of("WIFI"),
                new PricingRequest(new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("3000"),
                        new BigDecimal("9000"), new BigDecimal("2000"), BigDecimal.ZERO, BigDecimal.ZERO),
                List.of()
        ));

        adminService.approveProperty(admin.getId(), prop.id());

        var room = roomService.createRoom(ownerAuth.user().id(), prop.id(), new CreateRoomRequest(
                "101", RoomType.DOUBLE, 1, 2, "AC, Locker",
                null, null, null, null, true
        ));
        UUID bedId = room.beds().get(0).id();

        var searchRes = searchService.search("Bengaluru", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, true, true, null, 0, 10);
        assertFalse(searchRes.content().isEmpty());

        Instant checkIn = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant checkOut = checkIn.plus(2, ChronoUnit.DAYS);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> bookTask1 = () -> {
            ready.countDown();
            start.await();
            bookingService.createBooking(user1Auth.user().id(), new CreateBookingRequest(
                    prop.id(), bedId, checkIn, checkOut, StayType.SHORT_STAY, "Student One", "9876543211", false
            ));
            return true;
        };
        Callable<Boolean> bookTask2 = () -> {
            ready.countDown();
            start.await();
            bookingService.createBooking(user2Auth.user().id(), new CreateBookingRequest(
                    prop.id(), bedId, checkIn.plus(2, ChronoUnit.HOURS), checkOut.plus(2, ChronoUnit.HOURS),
                    StayType.SHORT_STAY, "Student Two", "9876543212", false
            ));
            return true;
        };

        Future<Boolean> f1 = executor.submit(bookTask1);
        Future<Boolean> f2 = executor.submit(bookTask2);
        ready.await();
        start.countDown();

        List<Boolean> successes = new ArrayList<>();
        int failures = 0;
        for (Future<Boolean> f : List.of(f1, f2)) {
            try {
                successes.add(f.get());
            } catch (ExecutionException ex) {
                failures++;
            }
        }
        executor.shutdown();

        assertEquals(1, successes.size(), "Exactly one overlapping concurrent booking must succeed");
        assertEquals(1, failures, "Exactly one overlapping concurrent booking must fail");

        // Half-open interval: back-to-back booking starting at exact previous checkOut must succeed
        var backToBack = bookingService.createBooking(user2Auth.user().id(), new CreateBookingRequest(
                prop.id(), bedId, checkOut, checkOut.plus(10, ChronoUnit.DAYS),
                StayType.LONG_STAY, "Student Two", "9876543212", false
        ));
        assertEquals(BookingStatus.PENDING, backToBack.bookingStatus());

        // Idempotent payment + duplicate webhook test
        var payment = paymentService.initiatePayment(user2Auth.user().id(),
                new InitiatePaymentRequest(backToBack.id(), PaymentMethod.UPI, "idem-key-1"), null);
        var paymentDuplicate = paymentService.initiatePayment(user2Auth.user().id(),
                new InitiatePaymentRequest(backToBack.id(), PaymentMethod.UPI, "idem-key-1"), null);
        assertEquals(payment.id(), paymentDuplicate.id());

        var verified = paymentService.verifyPayment(user2Auth.user().id(), payment.id(),
                new VerifyPaymentRequest("txn_ok_1", null, false, null));
        assertEquals(PaymentStatus.SUCCESS, verified.paymentStatus());

        var webhookDup = paymentService.processWebhook(
                new WebhookPayload(payment.paymentReference(), "txn_ok_1", PaymentStatus.SUCCESS, null), null);
        assertEquals(PaymentStatus.SUCCESS, webhookDup.paymentStatus());
    }
}
