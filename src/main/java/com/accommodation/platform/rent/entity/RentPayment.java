package com.accommodation.platform.rent.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rent_payments")
public class RentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rent_schedule_id", nullable = false)
    private UUID rentScheduleId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_reference", nullable = false, length = 60)
    private String paymentReference;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    @PrePersist
    void onCreate() {
        this.paidAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRentScheduleId() { return rentScheduleId; }
    public void setRentScheduleId(UUID rentScheduleId) { this.rentScheduleId = rentScheduleId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public Instant getPaidAt() { return paidAt; }
}
