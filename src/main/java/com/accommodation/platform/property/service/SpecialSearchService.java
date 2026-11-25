package com.accommodation.platform.property.service;

import com.accommodation.platform.common.enums.GenderPolicy;
import com.accommodation.platform.common.enums.PropertyType;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.common.util.DateTimeUtils;
import com.accommodation.platform.property.dto.PropertyDtos.PropertySearchItemResponse;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class SpecialSearchService {

    private final PropertySearchService propertySearchService;
    private final PropertyRepository propertyRepository;

    public SpecialSearchService(
            PropertySearchService propertySearchService,
            PropertyRepository propertyRepository
    ) {
        this.propertySearchService = propertySearchService;
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PropertySearchItemResponse> searchEmergencyStays(
            String city,
            Double latitude,
            Double longitude,
            Double radiusKm,
            Instant checkIn,
            Instant checkOut,
            BigDecimal maxBudget,
            GenderPolicy genderPolicy,
            int page,
            int size
    ) {
        PagedResponse<PropertySearchItemResponse> base = propertySearchService.search(
                city, null, null, latitude, longitude, radiusKm,
                checkIn, checkOut, StayType.SHORT_STAY, null, maxBudget,
                null, null, genderPolicy, null, null, true, true,
                latitude != null ? "distance" : "price_asc", page, size
        );

        LocalTime arrivalIst = DateTimeUtils.toIst(checkIn).toLocalTime();
        boolean isLateNight = arrivalIst.isAfter(LocalTime.of(22, 0)) || arrivalIst.isBefore(LocalTime.of(6, 0));

        List<PropertySearchItemResponse> filtered = base.content().stream()
                .filter(item -> {
                    Property p = propertyRepository.findById(item.id()).orElse(null);
                    if (p == null || !p.isEmergencyBookingEnabled()) {
                        return false;
                    }
                    if (isLateNight && !p.isLateNightCheckInAllowed()) {
                        return false;
                    }
                    if (p.getLatestCheckInTime() != null && arrivalIst.isAfter(p.getLatestCheckInTime()) && !p.isLateNightCheckInAllowed()) {
                        return false;
                    }
                    return true;
                })
                .toList();

        return new PagedResponse<>(filtered, base.page(), base.size(), filtered.size(), filtered.isEmpty() ? 0 : 1);
    }

    @Transactional(readOnly = true)
    public List<InterviewModeResult> searchInterviewMode(
            double interviewLat,
            double interviewLon,
            double radiusKm,
            Instant checkIn,
            Instant checkOut,
            BigDecimal maxBudget,
            GenderPolicy genderPolicy
    ) {
        PagedResponse<PropertySearchItemResponse> base = propertySearchService.search(
                null, null, null, interviewLat, interviewLon, radiusKm,
                checkIn, checkOut, StayType.SHORT_STAY, null, maxBudget,
                null, null, genderPolicy, null, null, true, true,
                "distance", 0, 30
        );

        return base.content().stream()
                .map(item -> new InterviewModeResult(
                        item.id(),
                        item.name(),
                        item.address(),
                        item.city(),
                        item.propertyType(),
                        item.genderPolicy(),
                        item.verificationStatus(),
                        item.latitude(),
                        item.longitude(),
                        item.distanceKm(),
                        item.availableBedCount(),
                        item.startingPrice(),
                        checkIn,
                        checkOut
                ))
                .toList();
    }

    public record InterviewModeResult(
            UUID propertyId,
            String propertyName,
            String address,
            String city,
            PropertyType propertyType,
            GenderPolicy genderPolicy,
            VerificationStatus verificationStatus,
            Double latitude,
            Double longitude,
            Double distanceKm,
            long availableBeds,
            BigDecimal startingPrice,
            Instant checkIn,
            Instant checkOut
    ) {}
}
