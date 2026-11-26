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
        BigDecimal netRent = booking.getBaseAmount()
                .subtract(booking.getDiscountAmount())
                .add(booking.getExtraCharges())
                .max(BigDecimal.ZERO);
        BigDecimal monthlyAmount = netRent
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
        syncScheduleWithBookingPayments(booking, created);
        return created;
    }

    @Transactional
    public void syncScheduleWithBookingPayments(Booking booking) {
        if (booking.getStayType() != StayType.LONG_STAY) {
            return;
        }
        List<RentSchedule> schedules = scheduleRepo.findByBookingIdOrderByInstallmentNumberAsc(booking.getId());
        if (!schedules.isEmpty()) {
            syncScheduleWithBookingPayments(booking, schedules);
        }
    }

    private void syncScheduleWithBookingPayments(Booking booking, List<RentSchedule> schedules) {
        BigDecimal netRentPaid = booking.getAmountPaid()
                .subtract(booking.getDepositAmount())
                .max(BigDecimal.ZERO);
        BigDecimal alreadyAllocated = schedules.stream()
                .map(RentSchedule::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal toAllocate = netRentPaid.subtract(alreadyAllocated);
        if (toAllocate.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        for (RentSchedule rs : schedules) {
            if (toAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal dueLeft = rs.getAmountDue().subtract(rs.getAmountPaid());
            if (dueLeft.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal alloc = dueLeft.min(toAllocate);
                rs.setAmountPaid(rs.getAmountPaid().add(alloc));
                rs.setStatus(rs.getAmountPaid().compareTo(rs.getAmountDue()) >= 0 ? "PAID" : "PARTIALLY_PAID");
                scheduleRepo.save(rs);
                toAllocate = toAllocate.subtract(alloc);
            }
        }
    }

    @Transactional
    public void syncScheduleForExtension(Booking booking) {
        if (booking.getStayType() != StayType.LONG_STAY) {
            return;
        }
        List<RentSchedule> existing = scheduleRepo.findByBookingIdOrderByInstallmentNumberAsc(booking.getId());
        if (existing.isEmpty()) {
            return;
        }
        BigDecimal totalNetRent = booking.getBaseAmount()
                .subtract(booking.getDiscountAmount())
                .add(booking.getExtraCharges())
                .max(BigDecimal.ZERO);
        BigDecimal totalScheduled = existing.stream()
                .map(RentSchedule::getAmountDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal additionalRent = totalNetRent.subtract(totalScheduled);
        if (additionalRent.compareTo(BigDecimal.ZERO) <= 0) {
            syncScheduleWithBookingPayments(booking, existing);
            return;
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut()));
        int targetMonths = Math.max(existing.size(), (int) Math.ceil(days / 30.0));
        int newInstallments = Math.max(1, targetMonths - existing.size());
        BigDecimal perInstallment = additionalRent.divide(BigDecimal.valueOf(newInstallments), 2, java.math.RoundingMode.HALF_UP);
        var startDate = booking.getCheckIn().atZone(ZoneOffset.UTC).toLocalDate();
        int nextNum = existing.size() + 1;
        for (int i = 0; i < newInstallments; i++) {
            RentSchedule rs = new RentSchedule();
            rs.setBookingId(booking.getId());
            rs.setInstallmentNumber(nextNum + i);
            rs.setDueDate(startDate.plusMonths((nextNum + i) - 1L));
            rs.setAmountDue(perInstallment);
            rs.setStatus("PENDING");
            existing.add(scheduleRepo.save(rs));
        }
        syncScheduleWithBookingPayments(booking, existing);
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

        booking.setAmountPaid(booking.getAmountPaid().add(amount));
        bookingRepository.save(booking);

        return rp;
    }

    @Transactional
    public List<RentSchedule> getSchedule(UUID bookingId) {
        bookingRepository.findById(bookingId).ifPresent(this::syncScheduleWithBookingPayments);
        return scheduleRepo.findByBookingIdOrderByInstallmentNumberAsc(bookingId);
    }
}
