package com.accommodation.platform.deposit.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.deposit.entity.SecurityDeposit;
import com.accommodation.platform.deposit.repository.SecurityDepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SecurityDepositService {

    private final SecurityDepositRepository depositRepository;
    private final BookingRepository bookingRepository;

    public SecurityDepositService(SecurityDepositRepository depositRepository, BookingRepository bookingRepository) {
        this.depositRepository = depositRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public SecurityDeposit createOrGetDeposit(UUID bookingId) {
        return depositRepository.findByBookingId(bookingId).orElseGet(() -> {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
            SecurityDeposit d = new SecurityDeposit();
            d.setBookingId(bookingId);
            d.setDepositAmount(booking.getDepositAmount());
            d.setStatus("HELD");
            return depositRepository.save(d);
        });
    }

    @Transactional
    public SecurityDeposit requestRefund(UUID customerId, UUID bookingId) {
        Booking booking = findBooking(bookingId);
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Access denied");
        }
        SecurityDeposit d = createOrGetDeposit(bookingId);
        if (!"HELD".equals(d.getStatus())) {
            throw new BadRequestException("Deposit refund already requested or processed", "INVALID_DEPOSIT_STATE");
        }
        d.setStatus("REFUND_REQUESTED");
        return depositRepository.save(d);
    }

    @Transactional
    public SecurityDeposit settleDepositByOwner(UUID ownerId, UUID bookingId, BigDecimal deduction, String reason) {
        Booking booking = findBooking(bookingId);
        if (!booking.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Security deposit can only be settled after the guest completes their stay", "STAY_NOT_COMPLETED");
        }
        SecurityDeposit d = createOrGetDeposit(bookingId);
        if ("REFUNDED".equals(d.getStatus()) || "PARTIALLY_REFUNDED".equals(d.getStatus())) {
            throw new BadRequestException("Deposit has already been settled", "ALREADY_SETTLED");
        }
        BigDecimal ded = deduction != null ? deduction : BigDecimal.ZERO;
        if (ded.compareTo(BigDecimal.ZERO) < 0 || ded.compareTo(d.getDepositAmount()) > 0) {
            throw new BadRequestException("Invalid deduction amount", "INVALID_DEDUCTION");
        }
        if (ded.compareTo(BigDecimal.ZERO) > 0 && (reason == null || reason.isBlank())) {
            throw new BadRequestException("Deduction reason is required when deducting from deposit", "REASON_REQUIRED");
        }

        BigDecimal refunded = d.getDepositAmount().subtract(ded);
        d.setDeductedAmount(ded);
        d.setDeductionReason(reason);
        d.setRefundedAmount(refunded);
        d.setStatus(ded.compareTo(BigDecimal.ZERO) > 0 ? "PARTIALLY_REFUNDED" : "REFUNDED");
        return depositRepository.save(d);
    }

    @Transactional
    public SecurityDeposit disputeDeduction(UUID customerId, UUID bookingId) {
        Booking booking = findBooking(bookingId);
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Access denied");
        }
        SecurityDeposit d = createOrGetDeposit(bookingId);
        d.setDisputed(true);
        d.setStatus("DISPUTED");
        return depositRepository.save(d);
    }

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
    }
}
