package com.accommodation.platform.booking.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.dto.BookingDtos.CreateBookingRequest;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.payment.dto.PaymentDtos.RefundRequest;
import com.accommodation.platform.payment.dto.PaymentDtos.VerifyPaymentRequest;
import com.accommodation.platform.payment.entity.Payment;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.payment.service.MockPaymentGateway;
import com.accommodation.platform.payment.service.PaymentService;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingAndPaymentUnitTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BedRepository bedRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private PaymentRepository paymentRepository;

    private AppProperties testProps() {
        return new AppProperties(
                "Asia/Kolkata",
                new AppProperties.Cors(List.of("*")),
                new AppProperties.Jwt("test-secret-key-at-least-32-bytes-long", 900000L, 604800000L),
                new AppProperties.Booking(15, 24, 24, new BigDecimal("20")),
                new AppProperties.Payment("MOCK", "webhook-secret", "", ""),
                new AppProperties.RateLimit(20)
        );
    }

    @Test
    void rejectsBookingOnUnverifiedProperty() {
        AppProperties props = testProps();
        BookingService bookingService = new BookingService(
                bookingRepository, bedRepository, userRepository,
                new AvailabilityService(null, bedRepository, bookingRepository, null),
                new PricingCalculationService(props),
                notificationService, auditLogService, props
        );

        UUID userId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setGender(Gender.MALE);

        Property property = new Property();
        property.setId(propertyId);
        property.setVerificationStatus(VerificationStatus.PENDING);
        property.setListingStatus(ListingStatus.ACTIVE);

        Room room = new Room();
        room.setProperty(property);

        Bed bed = new Bed();
        bed.setRoom(room);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bedRepository.findByIdForUpdate(bedId)).thenReturn(Optional.of(bed));

        CreateBookingRequest req = new CreateBookingRequest(
                propertyId, bedId,
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(2, ChronoUnit.DAYS),
                StayType.SHORT_STAY, "Rahul", "9876543210", false
        );

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(userId, req));
    }

    @Test
    void preventsNonOwnerFromApprovingBooking() {
        AppProperties props = testProps();
        BookingService bookingService = new BookingService(
                bookingRepository, bedRepository, userRepository,
                new AvailabilityService(null, bedRepository, bookingRepository, null),
                new PricingCalculationService(props),
                notificationService, auditLogService, props
        );

        UUID realOwnerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        User realOwner = new User();
        realOwner.setId(realOwnerId);
        Property property = new Property();
        property.setOwner(realOwner);

        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setBookingStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class, () -> bookingService.approveBookingByOwner(otherOwnerId, bookingId));
    }

    @Test
    void paymentVerificationIsIdempotentWhenAlreadySucceeded() {
        AppProperties props = testProps();
        PaymentService paymentService = new PaymentService(
                paymentRepository, bookingRepository, List.of(new MockPaymentGateway()),
                notificationService, auditLogService, props
        );

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookingReference("BK-20260101-ABCDEF");

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentReference("PAY-20260101-12345678");
        payment.setProvider(PaymentProvider.MOCK);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setPaymentStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        var res = paymentService.verifyPayment(userId, paymentId, new VerifyPaymentRequest("txn_1", null, false, null));
        assertEquals(PaymentStatus.SUCCESS, res.paymentStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void rejectsRefundExceedingPaymentAmount() {
        AppProperties props = testProps();
        PaymentService paymentService = new PaymentService(
                paymentRepository, bookingRepository, List.of(new MockPaymentGateway()),
                notificationService, auditLogService, props
        );

        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        Booking booking = new Booking();
        booking.setUser(user);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setRefundAmount(new BigDecimal("200.00"));
        payment.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class,
                () -> paymentService.refundPayment(userId, UserRole.CUSTOMER, paymentId, new RefundRequest(new BigDecimal("400.00"), "test")));
    }
}
