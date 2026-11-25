package com.accommodation.platform.rent.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.util.ReferenceGenerator;
import com.accommodation.platform.rent.entity.RentPayment;
import com.accommodation.platform.rent.entity.RentSchedule;
import com.accommodation.platform.rent.repository.RentPaymentRepository;
import com.accommodation.platform.rent.repository.RentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RentService {

    private final RentScheduleRepository scheduleRepo;
    private final RentPaymentRepository paymentRepo;
    private final BookingRepository bookingRepository;

    public RentService(
            RentScheduleRepository scheduleRepo,
            RentPaymentRepository paymentRepo,
            BookingRepository bookingRepository
    ) {
        this.scheduleRepo = scheduleRepo;
        this.paymentRepo = paymentRepo;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public List<RentSchedule> generateMonthlySchedule(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (booking.getStayType() != StayType.LONG_STAY) {
            throw new BadRequestException("Rent schedules apply only to LONG_STAY bookings", "INVALID_STAY_TYPE");
        }
        List<RentSchedule> existing = scheduleRepo.findByBookingIdOrderByInstallmentNumberAsc(bookingId);
        if (!existing.isEmpty()) {
            return existing;
        }

        long days = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
        int months = Math.max(1, (int) Math.ceil(days / 30.0));
        BigDecimal monthlyAmount = booking.getBaseAmount()
                .divide(BigDecimal.valueOf(months), 2, java.math.RoundingMode.HALF_UP);

        List<RentSchedule> created = new ArrayList<>();
        var startDate = booking.getCheckIn().atZone(ZoneOffset.UTC).toLocalDate();
        for (int i = 1; i <= months; i++) {
            RentSchedule rs = new RentSchedule();
            rs.setBookingId(bookingId);
            rs.setInstallmentNumber(i);
            rs.setDueDate(startDate.plusMonths(i - 1L));
            rs.setAmountDue(monthlyAmount);
            rs.setStatus("PENDING");
            created.add(scheduleRepo.save(rs));
        }
        return created;
    }

    @Transactional
    public RentPayment recordRentPayment(UUID userId, UUID scheduleId, BigDecimal amount) {
        RentSchedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rent schedule not found", "RENT_SCHEDULE_NOT_FOUND"));
        Booking booking = bookingRepository.findById(schedule.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(userId) && !booking.getProperty().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be positive", "INVALID_AMOUNT");
        }
        BigDecimal remaining = schedule.getAmountDue().subtract(schedule.getAmountPaid());
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException("Payment exceeds remaining rent due", "EXCEEDS_DUE");
        }

        RentPayment rp = new RentPayment();
        rp.setRentScheduleId(scheduleId);
        rp.setAmount(amount);
        rp.setPaymentReference(ReferenceGenerator.paymentReference());
        rp = paymentRepo.save(rp);

        schedule.setAmountPaid(schedule.getAmountPaid().add(amount));
        schedule.setStatus(schedule.getAmountPaid().compareTo(schedule.getAmountDue()) >= 0 ? "PAID" : "PARTIALLY_PAID");
        scheduleRepo.save(schedule);

        return rp;
    }

    @Transactional(readOnly = true)
    public List<RentSchedule> getSchedule(UUID bookingId) {
        return scheduleRepo.findByBookingIdOrderByInstallmentNumberAsc(bookingId);
    }
}
