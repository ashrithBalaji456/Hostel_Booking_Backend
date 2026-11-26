package com.accommodation.platform.booking.repository;

import com.accommodation.platform.booking.entity.BedChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BedChangeRequestRepository extends JpaRepository<BedChangeRequest, UUID> {

    @Query("""
            SELECT r FROM BedChangeRequest r
            JOIN FETCH r.booking b
            JOIN FETCH r.property p
            JOIN FETCH r.customer c
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cr
            JOIN FETCH r.requestedBed rb
            JOIN FETCH rb.room rr
            WHERE b.id = :bookingId
            ORDER BY r.createdAt DESC
            """)
    List<BedChangeRequest> findDetailedByBookingId(@Param("bookingId") UUID bookingId);

    @Query("""
            SELECT r FROM BedChangeRequest r
            JOIN FETCH r.booking b
            JOIN FETCH r.property p
            JOIN FETCH r.customer c
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cr
            JOIN FETCH r.requestedBed rb
            JOIN FETCH rb.room rr
            WHERE p.owner.id = :ownerId
            ORDER BY r.createdAt DESC
            """)
    List<BedChangeRequest> findDetailedByOwnerId(@Param("ownerId") UUID ownerId);

    boolean existsByBookingIdAndStatus(UUID bookingId, String status);
}
