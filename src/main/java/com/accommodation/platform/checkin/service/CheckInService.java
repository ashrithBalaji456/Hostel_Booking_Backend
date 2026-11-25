package com.accommodation.platform.checkin.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.entity.BookingStatusHistory;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.repository.BookingStatusHistoryRepository;
import com.accommodation.platform.checkin.entity.CheckInRecord;
import com.accommodation.platform.checkin.entity.CheckOutRecord;
import com.accommodation.platform.checkin.repository.CheckInRecordRepository;
import com.accommodation.platform.checkin.repository.CheckOutRecordRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CheckInService {

    private final BookingRepository bookingRepository;
    private final CheckInRecordRepository checkInRepo;
    private final CheckOutRecordRepository checkOutRepo;
    private final BookingStatusHistoryRepository historyRepo;

    public CheckInService(
            BookingRepository bookingRepository,
            CheckInRecordRepository checkInRepo,
            CheckOutRecordRepository checkOutRepo,
            BookingStatusHistoryRepository historyRepo
    ) {
        this.bookingRepository = bookingRepository;
        this.checkInRepo = checkInRepo;
        this.checkOutRepo = checkOutRepo;
        this.historyRepo = historyRepo;
    }

    @Transactional
    public CheckInRecord checkInByOwner(UUID ownerId, UUID bookingId, String notes) {
        Booking booking = findOwnedBooking(ownerId, bookingId);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be checked in", "INVALID_BOOKING_STATE");
        }
        if (checkInRepo.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("Booking is already checked in", "ALREADY_CHECKED_IN");
        }

        Instant now = Instant.now();
        CheckInRecord record = new CheckInRecord();
        record.setBookingId(bookingId);
        record.setCheckedInAt(now);
        record.setEarlyCheckin(now.isBefore(booking.getCheckIn()));
        record.setVerifiedByOwner(true);
        record.setNotes(notes);
        record = checkInRepo.save(record);

        booking.setStayStatus("CHECKED_IN");
        bookingRepository.save(booking);
        historyRepo.save(new BookingStatusHistory(bookingId, BookingStatus.CONFIRMED, BookingStatus.CONFIRMED, ownerId, "Guest checked in"));
        return record;
    }

    @Transactional
    public CheckInRecord confirmCheckInByCustomer(UUID customerId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Access denied");
        }
        CheckInRecord record = checkInRepo.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Check-in record not found", "CHECKIN_NOT_FOUND"));
        record.setVerifiedByCustomer(true);
        return checkInRepo.save(record);
    }

    @Transactional
    public CheckOutRecord checkOutByOwner(UUID ownerId, UUID bookingId, BigDecimal lateFee, String notes) {
        Booking booking = findOwnedBooking(ownerId, bookingId);
        if (!"CHECKED_IN".equals(booking.getStayStatus())) {
            throw new BadRequestException("Guest must be checked in before check-out", "INVALID_STAY_STATE");
        }
        if (checkOutRepo.findByBookingId(bookingId).isPresent()) {
            throw new ConflictException("Booking is already checked out", "ALREADY_CHECKED_OUT");
        }

        Instant now = Instant.now();
        CheckOutRecord record = new CheckOutRecord();
        record.setBookingId(bookingId);
        record.setCheckedOutAt(now);
        record.setLateCheckout(now.isAfter(booking.getCheckOut()));
        record.setLateCheckoutFee(lateFee != null ? lateFee : BigDecimal.ZERO);
        record.setVerifiedByOwner(true);
        record.setNotes(notes);
        record = checkOutRepo.save(record);

        booking.setStayStatus("CHECKED_OUT");
        booking.setBookingStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        historyRepo.save(new BookingStatusHistory(bookingId, BookingStatus.CONFIRMED, BookingStatus.COMPLETED, ownerId, "Guest checked out"));
        return record;
    }

    @Transactional
    public void markNoShow(UUID ownerId, UUID bookingId, String notes) {
        Booking booking = findOwnedBooking(ownerId, bookingId);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be marked as no-show", "INVALID_BOOKING_STATE");
        }
        booking.setStayStatus("NO_SHOW");
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(notes != null ? notes : "Guest did not show up");
        bookingRepository.save(booking);
        historyRepo.save(new BookingStatusHistory(bookingId, BookingStatus.CONFIRMED, BookingStatus.CANCELLED, ownerId, "Marked NO_SHOW"));
    }

    private Booking findOwnedBooking(UUID ownerId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        return booking;
    }
}
