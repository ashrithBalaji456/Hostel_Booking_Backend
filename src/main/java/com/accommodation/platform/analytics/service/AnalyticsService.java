package com.accommodation.platform.analytics.service;

import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.dispute.repository.BookingDisputeRepository;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.safety.repository.SafetyComplaintRepository;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingDisputeRepository disputeRepository;
    private final SafetyComplaintRepository complaintRepository;

    public AnalyticsService(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            BookingDisputeRepository disputeRepository,
            SafetyComplaintRepository complaintRepository
    ) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.disputeRepository = disputeRepository;
        this.complaintRepository = complaintRepository;
    }

    @Transactional(readOnly = true)
    public OwnerAnalyticsResponse getOwnerAnalytics(UUID ownerId) {
        long properties = propertyRepository.countByOwnerId(ownerId);
        long confirmed = bookingRepository.countByPropertyOwnerIdAndBookingStatus(ownerId, BookingStatus.CONFIRMED);
        long completed = bookingRepository.countByPropertyOwnerIdAndBookingStatus(ownerId, BookingStatus.COMPLETED);
        long cancelled = bookingRepository.countByPropertyOwnerIdAndBookingStatus(ownerId, BookingStatus.CANCELLED);
        BigDecimal revenue = paymentRepository.calculateOwnerNetEarnings(ownerId);

        return new OwnerAnalyticsResponse(
                properties,
                confirmed + completed + cancelled,
                confirmed,
                completed,
                cancelled,
                revenue != null ? revenue : BigDecimal.ZERO
        );
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics() {
        BigDecimal totalRevenue = paymentRepository.calculateTotalNetRevenue();
        return new AdminAnalyticsResponse(
                userRepository.countByRole(UserRole.CUSTOMER),
                userRepository.countByRole(UserRole.PROPERTY_OWNER),
                propertyRepository.count(),
                propertyRepository.countByVerificationStatus(VerificationStatus.APPROVED),
                bookingRepository.count(),
                bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED),
                bookingRepository.countByBookingStatus(BookingStatus.CANCELLED),
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                disputeRepository.countByStatus("OPEN"),
                complaintRepository.countByStatus("OPEN")
        );
    }

    public record OwnerAnalyticsResponse(
            long totalProperties,
            long totalBookings,
            long confirmedBookings,
            long completedBookings,
            long cancelledBookings,
            BigDecimal netRevenue
    ) {}

    public record AdminAnalyticsResponse(
            long totalCustomers,
            long totalOwners,
            long totalProperties,
            long approvedProperties,
            long totalBookings,
            long confirmedBookings,
            long cancelledBookings,
            BigDecimal netBookingRevenue,
            long openDisputes,
            long openSafetyComplaints
    ) {}
}
