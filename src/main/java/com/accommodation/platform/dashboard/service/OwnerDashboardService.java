package com.accommodation.platform.dashboard.service;

import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dashboard.dto.DashboardDtos.OwnerDashboardResponse;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.property.repository.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OwnerDashboardService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    public OwnerDashboardService(
            PropertyRepository propertyRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            BookingService bookingService
    ) {
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getOwnerBookings(UUID ownerId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<Booking> bookings = (status != null)
                ? bookingRepository.findByPropertyOwnerIdAndBookingStatusOrderByCreatedAtDesc(ownerId, status, pageable)
                : bookingRepository.findByPropertyOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
        return PagedResponse.from(bookings.map(bookingService::toResponse));
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(UUID ownerId) {
        long totalProperties = propertyRepository.countByOwnerId(ownerId);
        long pendingBookings = bookingRepository.countByPropertyOwnerIdAndBookingStatus(ownerId, BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByPropertyOwnerIdAndBookingStatus(ownerId, BookingStatus.CONFIRMED);
        BigDecimal netEarnings = paymentRepository.calculateOwnerNetEarnings(ownerId);

        Instant now = Instant.now();
        List<BookingResponse> upcomingCheckIns = bookingRepository
                .findUpcomingCheckInsForOwner(ownerId, now, now.plus(7, ChronoUnit.DAYS))
                .stream()
                .map(bookingService::toResponse)
                .toList();

        return new OwnerDashboardResponse(
                totalProperties,
                pendingBookings,
                confirmedBookings,
                netEarnings != null ? netEarnings : BigDecimal.ZERO,
                upcomingCheckIns
        );
    }
}
