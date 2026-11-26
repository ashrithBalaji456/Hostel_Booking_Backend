package com.accommodation.platform.availability.dto;

import com.accommodation.platform.common.enums.RoomType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AvailabilityDtos {

    private AvailabilityDtos() {}

    public record AvailableBedInfo(
            UUID bedId,
            String bedNumber,
            UUID roomId,
            String roomNumber,
            RoomType roomType,
            int floor,
            BigDecimal hourlyRate,
            BigDecimal nightlyRate,
            BigDecimal weeklyRate,
            BigDecimal monthlyRate
    ) {}

    public record OccupiedBedInfo(
            UUID bedId,
            UUID roomId,
            String roomNumber,
            String bedNumber,
            String bookingStatus,
            String occupantName,
            String occupantAvatar,
            String occupationType,
            String organizationName,
            String courseOrRole
    ) {}

    public record PropertyAvailabilityResponse(
            UUID propertyId,
            Instant checkIn,
            Instant checkOut,
            int totalBeds,
            int availableBedsCount,
            List<AvailableBedInfo> availableBeds,
            List<OccupiedBedInfo> occupiedBeds
    ) {}
}
