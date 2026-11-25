package com.accommodation.platform.checkin.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkout_records")
public class CheckOutRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "checked_out_at", nullable = false)
    private Instant checkedOutAt;

    @Column(name = "late_checkout", nullable = false)
    private boolean lateCheckout = false;

    @Column(name = "late_checkout_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lateCheckoutFee = BigDecimal.ZERO;

    @Column(name = "verified_by_owner", nullable = false)
    private boolean verifiedByOwner = false;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public Instant getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(Instant checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public boolean isLateCheckout() { return lateCheckout; }
    public void setLateCheckout(boolean lateCheckout) { this.lateCheckout = lateCheckout; }
    public BigDecimal getLateCheckoutFee() { return lateCheckoutFee; }
    public void setLateCheckoutFee(BigDecimal lateCheckoutFee) { this.lateCheckoutFee = lateCheckoutFee; }
    public boolean isVerifiedByOwner() { return verifiedByOwner; }
    public void setVerifiedByOwner(boolean verifiedByOwner) { this.verifiedByOwner = verifiedByOwner; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
