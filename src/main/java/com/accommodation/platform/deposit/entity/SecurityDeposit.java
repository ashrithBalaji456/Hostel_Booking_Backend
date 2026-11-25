package com.accommodation.platform.deposit.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_deposits")
public class SecurityDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "deducted_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "deduction_reason", length = 500)
    private String deductionReason;

    @Column(nullable = false)
    private boolean disputed = false;

    @Column(nullable = false, length = 30)
    private String status = "HELD"; // HELD, REFUND_REQUESTED, REFUNDED, PARTIALLY_REFUNDED, DISPUTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getDeductedAmount() { return deductedAmount; }
    public void setDeductedAmount(BigDecimal deductedAmount) { this.deductedAmount = deductedAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public String getDeductionReason() { return deductionReason; }
    public void setDeductionReason(String deductionReason) { this.deductionReason = deductionReason; }
    public boolean isDisputed() { return disputed; }
    public void setDisputed(boolean disputed) { this.disputed = disputed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
