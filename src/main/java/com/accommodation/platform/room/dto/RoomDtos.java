package com.accommodation.platform.room.dto;

import com.accommodation.platform.bed.dto.BedDtos.BedResponse;
import com.accommodation.platform.common.enums.RoomStatus;
import com.accommodation.platform.common.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class RoomDtos {

    private RoomDtos() {}

    public record CreateRoomRequest(
            @NotBlank(message = "Room number is required")
            String roomNumber,

            @NotNull(message = "Room type is required")
            RoomType roomType,

            int floor,

            @Min(value = 1, message = "Room capacity must be at least 1")
            int capacity,

            String amenities,

            @DecimalMin(value = "0.00")
            BigDecimal hourlyRate,

            @DecimalMin(value = "0.00")
            BigDecimal nightlyRate,

            @DecimalMin(value = "0.00")
            BigDecimal weeklyRate,

            @DecimalMin(value = "0.00")
            BigDecimal monthlyRate,

            boolean autoCreateBeds
    ) {}

    public record RoomResponse(
            UUID id,
            UUID propertyId,
            String roomNumber,
            RoomType roomType,
            int floor,
            int capacity,
            String amenities,
            RoomStatus status,
            BigDecimal hourlyRate,
            BigDecimal nightlyRate,
            BigDecimal weeklyRate,
            BigDecimal monthlyRate,
            List<BedResponse> beds
    ) {}
}
