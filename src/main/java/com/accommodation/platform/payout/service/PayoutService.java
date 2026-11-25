package com.accommodation.platform.payout.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.payout.entity.OwnerCommission;
import com.accommodation.platform.payout.entity.OwnerPayout;
import com.accommodation.platform.payout.repository.OwnerCommissionRepository;
import com.accommodation.platform.payout.repository.OwnerPayoutRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class PayoutService {

    private static final BigDecimal DEFAULT_COMMISSION_PCT = new BigDecimal("10.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final OwnerCommissionRepository commissionRepository;
    private final OwnerPayoutRepository payoutRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogService auditLogService;

    public PayoutService(
            OwnerCommissionRepository commissionRepository,
            OwnerPayoutRepository payoutRepository,
            PaymentRepository paymentRepository,
            AuditLogService auditLogService
    ) {
        this.commissionRepository = commissionRepository;
        this.payoutRepository = payoutRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OwnerCommission setCommission(UUID adminId, UUID ownerId, BigDecimal percentage) {
        if (percentage == null || percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("50")) > 0) {
            throw new BadRequestException("Commission must be between 0% and 50%", "INVALID_COMMISSION");
        }
        OwnerCommission c = commissionRepository.findByOwnerId(ownerId).orElseGet(() -> {
            OwnerCommission nc = new OwnerCommission();
            nc.setOwnerId(ownerId);
            return nc;
        });
        c.setCommissionPercentage(percentage);
        c.setUpdatedBy(adminId);
        c.setUpdatedAt(Instant.now());
        c = commissionRepository.save(c);
        auditLogService.record(adminId, null, "COMMISSION_UPDATED", "OwnerCommission", ownerId.toString(), "Pct=" + percentage);
        return c;
    }

    @Transactional
    public OwnerPayout requestPayout(UUID ownerId, BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payout amount must be positive", "INVALID_PAYOUT_AMOUNT");
        }
        BigDecimal totalNetEarnings = paymentRepository.calculateOwnerNetEarnings(ownerId);
        if (totalNetEarnings == null || grossAmount.compareTo(totalNetEarnings) > 0) {
            throw new BadRequestException("Requested payout exceeds eligible earnings", "INSUFFICIENT_EARNINGS");
        }

        BigDecimal pct = commissionRepository.findByOwnerId(ownerId)
                .map(OwnerCommission::getCommissionPercentage)
                .orElse(DEFAULT_COMMISSION_PCT);
        BigDecimal commission = grossAmount.multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal netPayable = grossAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);

        OwnerPayout payout = new OwnerPayout();
        payout.setOwnerId(ownerId);
        payout.setGrossAmount(grossAmount);
        payout.setCommissionAmount(commission);
        payout.setRefundAdjustments(BigDecimal.ZERO);
        payout.setNetPayable(netPayable);
        payout.setStatus("REQUESTED");
        return payoutRepository.save(payout);
    }

    @Transactional
    public OwnerPayout settlePayout(UUID adminId, UUID payoutId, String settlementReference) {
        OwnerPayout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found", "PAYOUT_NOT_FOUND"));
        if (!"REQUESTED".equals(payout.getStatus())) {
            throw new BadRequestException("Payout is already processed", "INVALID_PAYOUT_STATE");
        }
        payout.setStatus("SETTLED");
        payout.setSettlementReference(settlementReference);
        payout.setProcessedAt(Instant.now());
        payout = payoutRepository.save(payout);

        auditLogService.record(adminId, null, "PAYOUT_SETTLED", "OwnerPayout", payoutId.toString(),
                "NetPayable=" + payout.getNetPayable() + ", Ref=" + settlementReference);
        return payout;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OwnerPayout> getOwnerPayouts(UUID ownerId, int page, int size) {
        return PagedResponse.from(
                payoutRepository.findByOwnerIdOrderByRequestedAtDesc(
                        ownerId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
                )
        );
    }
}
