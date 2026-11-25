package com.accommodation.platform.booking.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class BookingExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public BookingExpirationScheduler(BookingRepository bookingRepository, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void expireUnpaidBookings() {
        List<Booking> expired = bookingRepository.findExpiredPendingBookings(Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (Booking booking : expired) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            notificationService.notify(
                    booking.getUser(),
                    "Booking Expired",
                    "Your booking " + booking.getBookingReference() + " expired because payment or approval was not completed in time.",
                    NotificationType.BOOKING_EXPIRED
            );
        }
        log.info("Expired {} unpaid/unapproved pending bookings", expired.size());
    }
}
