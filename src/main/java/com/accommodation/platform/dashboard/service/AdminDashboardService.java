package com.accommodation.platform.dashboard.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dashboard.dto.DashboardDtos.AdminDashboardResponse;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.payment.dto.PaymentDtos.PaymentResponse;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.payment.service.PaymentService;
import com.accommodation.platform.property.dto.PropertyDtos.PropertyDetailResponse;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.property.service.PropertyService;
import com.accommodation.platform.review.entity.Review;
import com.accommodation.platform.review.repository.ReviewRepository;
import com.accommodation.platform.user.dto.UserDtos.UserProfileResponse;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import com.accommodation.platform.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AdminDashboardService(
            UserRepository userRepository,
            UserService userService,
            PropertyRepository propertyRepository,
            PropertyService propertyService,
            BookingRepository bookingRepository,
            BookingService bookingService,
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            ReviewRepository reviewRepository,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.propertyRepository = propertyRepository;
        this.propertyService = propertyService;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> listUsers(UserRole role, AccountStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("createdAt").descending());
        if (role != null && status != null) {
            return PagedResponse.from(userRepository.findByRoleAndAccountStatus(role, status, pageable).map(userService::toResponse));
        }
        if (role != null) {
            return PagedResponse.from(userRepository.findByRole(role, pageable).map(userService::toResponse));
        }
        if (status != null) {
            return PagedResponse.from(userRepository.findByAccountStatus(status, pageable).map(userService::toResponse));
        }
        return PagedResponse.from(userRepository.findAll(pageable).map(userService::toResponse));
    }

    @Transactional
    public UserProfileResponse updateUserStatus(UUID adminId, UUID userId, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));
        user.setAccountStatus(status);
        user = userRepository.save(user);
        auditLogService.record(adminId, null, "USER_STATUS_UPDATED", "User", userId.toString(), "Status=" + status);
        return userService.toResponse(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PropertyDetailResponse> listProperties(VerificationStatus verificationStatus, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("createdAt").descending());
        var pageData = (verificationStatus != null)
                ? propertyRepository.findByVerificationStatus(verificationStatus, pageable)
                : propertyRepository.findAll(pageable);
        return PagedResponse.from(pageData.map(p -> propertyService.getPropertyDetails(p.getId(), true)));
    }

    @Transactional
    public PropertyDetailResponse approveProperty(UUID adminId, UUID propertyId) {
        Property property = findProperty(propertyId);
        property.setVerificationStatus(VerificationStatus.APPROVED);
        property.setRejectionReason(null);
        propertyRepository.save(property);

        notificationService.notify(
                property.getOwner(),
                "Property Approved",
                "Your property '" + property.getName() + "' has been verified and approved.",
                NotificationType.PROPERTY_VERIFICATION
        );
        auditLogService.record(adminId, null, "PROPERTY_APPROVED", "Property", propertyId.toString(), property.getName());
        return propertyService.getPropertyDetails(propertyId, true);
    }

    @Transactional
    public PropertyDetailResponse rejectProperty(UUID adminId, UUID propertyId, String reason) {
        Property property = findProperty(propertyId);
        property.setVerificationStatus(VerificationStatus.REJECTED);
        property.setRejectionReason(reason);
        propertyRepository.save(property);

        notificationService.notify(
                property.getOwner(),
                "Property Verification Rejected",
                "Your property '" + property.getName() + "' was rejected: " + reason,
                NotificationType.PROPERTY_VERIFICATION
        );
        auditLogService.record(adminId, null, "PROPERTY_REJECTED", "Property", propertyId.toString(), "Reason=" + reason);
        return propertyService.getPropertyDetails(propertyId, true);
    }

    @Transactional
    public PropertyDetailResponse suspendProperty(UUID adminId, UUID propertyId) {
        Property property = findProperty(propertyId);
        property.setListingStatus(ListingStatus.SUSPENDED);
        propertyRepository.save(property);

        notificationService.notify(
                property.getOwner(),
                "Property Suspended",
                "Your property '" + property.getName() + "' has been suspended by an administrator.",
                NotificationType.PROPERTY_VERIFICATION
        );
        auditLogService.record(adminId, null, "PROPERTY_SUSPENDED", "Property", propertyId.toString(), property.getName());
        return propertyService.getPropertyDetails(propertyId, true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> listBookings(BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("createdAt").descending());
        var pageData = (status != null)
                ? bookingRepository.findByBookingStatusOrderByCreatedAtDesc(status, pageable)
                : bookingRepository.findAll(pageable);
        return PagedResponse.from(pageData.map(bookingService::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> listPayments(PaymentStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by("createdAt").descending());
        var pageData = (status != null)
                ? paymentRepository.findByPaymentStatusOrderByCreatedAtDesc(status, pageable)
                : paymentRepository.findAll(pageable);
        return PagedResponse.from(pageData.map(paymentService::toResponse));
    }

    @Transactional
    public void moderateReview(UUID adminId, UUID reviewId, boolean hidden) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found", "REVIEW_NOT_FOUND"));
        review.setHidden(hidden);
        reviewRepository.save(review);
        auditLogService.record(adminId, null, "REVIEW_MODERATED", "Review", reviewId.toString(), "Hidden=" + hidden);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        BigDecimal revenue = paymentRepository.calculateTotalNetRevenue();
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.CUSTOMER),
                userRepository.countByRole(UserRole.PROPERTY_OWNER),
                propertyRepository.count(),
                propertyRepository.countByVerificationStatus(VerificationStatus.PENDING),
                bookingRepository.count(),
                bookingRepository.countByBookingStatus(BookingStatus.CONFIRMED),
                revenue != null ? revenue : BigDecimal.ZERO
        );
    }

    private Property findProperty(UUID propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));
    }
}
