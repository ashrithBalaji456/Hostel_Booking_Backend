package com.accommodation.platform.review.repository;

import com.accommodation.platform.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBookingId(UUID bookingId);
    Page<Review> findByPropertyIdAndHiddenFalseOrderByCreatedAtDesc(UUID propertyId, Pageable pageable);
    long countByPropertyIdAndHiddenFalse(UUID propertyId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.property.id = :propertyId AND r.hidden = false")
    Double findAverageRatingByPropertyId(@Param("propertyId") UUID propertyId);
}
