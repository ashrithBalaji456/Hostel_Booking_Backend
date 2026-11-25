package com.accommodation.platform.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ReviewDtos {

    private ReviewDtos() {}

    public record CreateReviewRequest(
            @NotNull(message = "Booking ID is required")
            UUID bookingId,

            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            int rating,

            @Size(max = 2000)
            String comment
    ) {}

    public record UpdateReviewRequest(
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            int rating,

            @Size(max = 2000)
            String comment
    ) {}

    public record ReviewResponse(
            UUID id,
            UUID bookingId,
            UUID propertyId,
            UUID userId,
            String reviewerName,
            int rating,
            String comment,
            Instant createdAt
    ) {}
}
