package com.accommodation.platform.property.dto;

import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.room.dto.RoomDtos.RoomResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PropertyDtos {

    private PropertyDtos() {}

    public record PricingRequest(
            @DecimalMin(value = "0.00", message = "Hourly rate cannot be negative")
            BigDecimal hourlyRate,

            @NotNull(message = "Nightly rate is required")
            @DecimalMin(value = "1.00", message = "Nightly rate must be at least 1.00")
            BigDecimal nightlyRate,

            @DecimalMin(value = "0.00", message = "Weekly rate cannot be negative")
            BigDecimal weeklyRate,

            @DecimalMin(value = "0.00", message = "Monthly rate cannot be negative")
            BigDecimal monthlyRate,

            @DecimalMin(value = "0.00", message = "Security deposit cannot be negative")
            BigDecimal securityDeposit,

            @DecimalMin(value = "0.00", message = "Extra charges cannot be negative")
            BigDecimal extraCharges,

            @DecimalMin(value = "0.00")
            @DecimalMax(value = "90.00", message = "Discount cannot exceed 90%")
            BigDecimal discountPercentage
    ) {}

    public record ImageRequest(
            @NotBlank(message = "Image URL is required")
            @Size(max = 5000000)
            String imageUrl,

            @NotNull(message = "Image type is required")
            ImageType imageType,

            int displayOrder
    ) {}

    public record CreatePropertyRequest(
            @NotBlank(message = "Property name is required")
            @Size(min = 3, max = 200)
            String name,

            String description,

            @NotNull(message = "Property type is required")
            PropertyType propertyType,

            @NotBlank(message = "Address is required")
            String address,

            String locality,

            @NotBlank(message = "City is required")
            String city,

            @NotBlank(message = "State is required")
            String state,

            @NotBlank(message = "Pincode is required")
            @Pattern(regexp = "^[1-9]\\d{5}$", message = "Invalid Indian pincode")
            String pincode,

            @NotNull(message = "Latitude is required")
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,

            @NotNull(message = "Longitude is required")
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,

            @NotNull(message = "Gender policy is required")
            GenderPolicy genderPolicy,

            LocalTime checkInTime,
            LocalTime checkOutTime,
            boolean instantBookingEnabled,

            @Min(1)
            Integer minShortStayHours,

            @Min(1)
            Integer maxShortStayDays,

            @Min(1)
            Integer minLongStayDays,

            @Min(1)
            Integer maxLongStayMonths,

            @Min(0)
            Integer minDepositRefundMonths,

            Set<String> amenityCodes,

            @NotNull(message = "Pricing configuration is required")
            @Valid
            PricingRequest pricing,

            @Valid
            List<ImageRequest> images
    ) {}

    public record UpdatePropertyRequest(
            @NotBlank(message = "Property name is required")
            @Size(min = 3, max = 200)
            String name,

            String description,

            @NotNull(message = "Property type is required")
            PropertyType propertyType,

            @NotBlank(message = "Address is required")
            String address,

            String locality,

            @NotBlank(message = "City is required")
            String city,

            @NotBlank(message = "State is required")
            String state,

            @NotBlank(message = "Pincode is required")
            @Pattern(regexp = "^[1-9]\\d{5}$", message = "Invalid Indian pincode")
            String pincode,

            @NotNull(message = "Latitude is required")
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,

            @NotNull(message = "Longitude is required")
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,

            @NotNull(message = "Gender policy is required")
            GenderPolicy genderPolicy,

            LocalTime checkInTime,
            LocalTime checkOutTime,
            boolean instantBookingEnabled,
            Integer minShortStayHours,
            Integer maxShortStayDays,
            Integer minLongStayDays,
            Integer maxLongStayMonths,
            Integer minDepositRefundMonths,
            ListingStatus listingStatus,
            Set<String> amenityCodes,
            @Valid PricingRequest pricing,
            @Valid List<ImageRequest> images
    ) {}

    public record PricingResponse(
            BigDecimal hourlyRate,
            BigDecimal nightlyRate,
            BigDecimal weeklyRate,
            BigDecimal monthlyRate,
            BigDecimal securityDeposit,
            BigDecimal extraCharges,
            BigDecimal discountPercentage
    ) {}

    public record ImageResponse(
            UUID id,
            String imageUrl,
            ImageType imageType,
            int displayOrder
    ) {}

    public record PropertyDetailResponse(
            UUID id,
            UUID ownerId,
            String ownerName,
            String name,
            String description,
            PropertyType propertyType,
            String address,
            String locality,
            String city,
            String state,
            String pincode,
            @NotNull(message = "Latitude is required")
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
            Double latitude,

            @NotNull(message = "Longitude is required")
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
            Double longitude,
            GenderPolicy genderPolicy,
            LocalTime checkInTime,
            LocalTime checkOutTime,
            VerificationStatus verificationStatus,
            ListingStatus listingStatus,
            boolean instantBookingEnabled,
            int minShortStayHours,
            int maxShortStayDays,
            int minLongStayDays,
            int maxLongStayMonths,
            int minDepositRefundMonths,
            String rejectionReason,
            Set<String> amenities,
            PricingResponse pricing,
            List<ImageResponse> images,
            List<RoomResponse> rooms,
            Double averageRating,
            long reviewCount,
            Instant createdAt
    ) {}

    public record PropertySearchItemResponse(
            UUID id,
            String name,
            String address,
            String locality,
            String city,
            String pincode,
            PropertyType propertyType,
            GenderPolicy genderPolicy,
            VerificationStatus verificationStatus,
            Double latitude,
            Double longitude,
            Double distanceKm,
            BigDecimal startingPrice,
            long availableBedCount,
            Double averageRating,
            long reviewCount,
            String mainImage,
            List<String> imageUrls,
            List<StayType> availableStayTypes
    ) {}
}
