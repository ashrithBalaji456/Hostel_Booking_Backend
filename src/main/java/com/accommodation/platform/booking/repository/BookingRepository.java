package com.accommodation.platform.booking.repository;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    boolean existsByPropertyId(UUID propertyId);

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.bed.id = :bedId
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
              AND (b.bookingStatus = 'CONFIRMED'
                   OR (b.bookingStatus = 'PENDING' AND (b.expiresAt IS NULL OR b.expiresAt > :now)))
            """)
    boolean existsOverlappingBooking(
            @Param("bedId") UUID bedId,
            @Param("checkIn") Instant checkIn,
            @Param("checkOut") Instant checkOut,
            @Param("now") Instant now
    );

    @Query("""
            SELECT DISTINCT b.bed.id FROM Booking b
            WHERE b.property.id = :propertyId
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
              AND (b.bookingStatus = 'CONFIRMED'
                   OR (b.bookingStatus = 'PENDING' AND (b.expiresAt IS NULL OR b.expiresAt > :now)))
            """)
    Set<UUID> findOverlappingBedIdsForProperty(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") Instant checkIn,
            @Param("checkOut") Instant checkOut,
            @Param("now") Instant now
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.bookingStatus = 'PENDING'
              AND b.expiresAt IS NOT NULL
              AND b.expiresAt < :now
            """)
    List<Booking> findExpiredPendingBookings(@Param("now") Instant now);

    Page<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Booking> findByPropertyOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    Page<Booking> findByPropertyOwnerIdAndBookingStatusOrderByCreatedAtDesc(
            UUID ownerId, BookingStatus status, Pageable pageable
    );

    Page<Booking> findByBookingStatusOrderByCreatedAtDesc(BookingStatus status, Pageable pageable);

    long countByBookingStatus(BookingStatus status);

    long countByPropertyOwnerIdAndBookingStatus(UUID ownerId, BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.property.owner.id = :ownerId
              AND b.bookingStatus = 'CONFIRMED'
              AND b.checkIn >= :from
              AND b.checkIn <= :to
            ORDER BY b.checkIn ASC
            """)
    List<Booking> findUpcomingCheckInsForOwner(
            @Param("ownerId") UUID ownerId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
