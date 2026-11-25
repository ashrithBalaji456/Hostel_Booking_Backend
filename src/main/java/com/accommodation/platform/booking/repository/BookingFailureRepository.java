package com.accommodation.platform.booking.repository;

import com.accommodation.platform.booking.entity.BookingFailure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingFailureRepository extends JpaRepository<BookingFailure, UUID> {
    List<BookingFailure> findByBookingId(UUID bookingId);
}
