package com.accommodation.platform.booking.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_failures")
public class BookingFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "failure_reason", nullable = false, length = 50)
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "alternative_property_id")
    private UUID alternativePropertyId;

    @Column(name = "alternative_bed_id")
    private UUID alternativeBedId;

    @Column(name = "customer_accepted_alternative")
    private Boolean customerAcceptedAlternative;

    @Column(name = "resolution_status", nullable = false, length = 30)
    private String resolutionStatus = "OPEN";

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
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getAlternativePropertyId() { return alternativePropertyId; }
    public void setAlternativePropertyId(UUID alternativePropertyId) { this.alternativePropertyId = alternativePropertyId; }
    public UUID getAlternativeBedId() { return alternativeBedId; }
    public void setAlternativeBedId(UUID alternativeBedId) { this.alternativeBedId = alternativeBedId; }
    public Boolean getCustomerAcceptedAlternative() { return customerAcceptedAlternative; }
    public void setCustomerAcceptedAlternative(Boolean customerAcceptedAlternative) { this.customerAcceptedAlternative = customerAcceptedAlternative; }
    public String getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(String resolutionStatus) { this.resolutionStatus = resolutionStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
