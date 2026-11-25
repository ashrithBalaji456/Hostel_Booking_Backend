package com.accommodation.platform.payment.repository;

import com.accommodation.platform.common.enums.PaymentStatus;
import com.accommodation.platform.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);
    Page<Payment> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount - p.refundAmount), 0) FROM Payment p WHERE p.paymentStatus IN ('SUCCESS', 'PARTIALLY_REFUNDED')")
    BigDecimal calculateTotalNetRevenue();

    @Query("SELECT COALESCE(SUM(p.amount - p.refundAmount), 0) FROM Payment p WHERE p.booking.property.owner.id = :ownerId AND p.paymentStatus IN ('SUCCESS', 'PARTIALLY_REFUNDED')")
    BigDecimal calculateOwnerNetEarnings(@Param("ownerId") UUID ownerId);
}
