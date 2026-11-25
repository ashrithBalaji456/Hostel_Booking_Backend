package com.accommodation.platform.booking.entity;

import com.accommodation.platform.common.enums.BookingStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private BookingStatus toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public BookingStatusHistory() {}

    public BookingStatusHistory(UUID bookingId, BookingStatus fromStatus, BookingStatus toStatus, UUID changedBy, String reason) {
        this.bookingId = bookingId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public BookingStatus getFromStatus() { return fromStatus; }
    public BookingStatus getToStatus() { return toStatus; }
    public UUID getChangedBy() { return changedBy; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
