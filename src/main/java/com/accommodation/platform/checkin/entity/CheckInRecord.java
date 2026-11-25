package com.accommodation.platform.checkin.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checkin_records")
public class CheckInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "checked_in_at", nullable = false)
    private Instant checkedInAt;

    @Column(name = "early_checkin", nullable = false)
    private boolean earlyCheckin = false;

    @Column(name = "verified_by_owner", nullable = false)
    private boolean verifiedByOwner = false;

    @Column(name = "verified_by_customer", nullable = false)
    private boolean verifiedByCustomer = false;

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
    public Instant getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Instant checkedInAt) { this.checkedInAt = checkedInAt; }
    public boolean isEarlyCheckin() { return earlyCheckin; }
    public void setEarlyCheckin(boolean earlyCheckin) { this.earlyCheckin = earlyCheckin; }
    public boolean isVerifiedByOwner() { return verifiedByOwner; }
    public void setVerifiedByOwner(boolean verifiedByOwner) { this.verifiedByOwner = verifiedByOwner; }
    public boolean isVerifiedByCustomer() { return verifiedByCustomer; }
    public void setVerifiedByCustomer(boolean verifiedByCustomer) { this.verifiedByCustomer = verifiedByCustomer; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
