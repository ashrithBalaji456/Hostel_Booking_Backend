package com.accommodation.platform.bed.dto;

import com.accommodation.platform.common.enums.BedStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public final class BedDtos {

    private BedDtos() {}

    public record CreateBedRequest(
            @NotNull(message = "Room ID is required")
            UUID roomId,

            @NotBlank(message = "Bed number is required")
            String bedNumber
    ) {}

    public record UpdateBedAvailabilityRequest(
            @NotNull(message = "Bed ID is required")
            UUID bedId,

            @NotNull(message = "Bed status is required")
            BedStatus status
    ) {}

    public record BedResponse(
            UUID id,
            UUID roomId,
            String bedNumber,
            BedStatus status
    ) {}
}
