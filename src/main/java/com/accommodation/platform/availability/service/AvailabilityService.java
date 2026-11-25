package com.accommodation.platform.availability.service;

import com.accommodation.platform.availability.dto.AvailabilityDtos.AvailableBedInfo;
import com.accommodation.platform.availability.dto.AvailabilityDtos.PropertyAvailabilityResponse;
import com.accommodation.platform.availability.repository.AvailabilityBlockRepository;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.enums.RoomStatus;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.room.entity.Room;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AvailabilityService {

    private final PropertyRepository propertyRepository;
    private final BedRepository bedRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityBlockRepository blockRepository;

    public AvailabilityService(
            PropertyRepository propertyRepository,
            BedRepository bedRepository,
            BookingRepository bookingRepository,
            AvailabilityBlockRepository blockRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.bedRepository = bedRepository;
        this.bookingRepository = bookingRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public PropertyAvailabilityResponse checkPropertyAvailability(UUID propertyId, Instant checkIn, Instant checkOut) {
        validateTimeWindow(checkIn, checkOut);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));

        if (property.getVerificationStatus() != VerificationStatus.APPROVED
                || property.getListingStatus() != ListingStatus.ACTIVE) {
            return new PropertyAvailabilityResponse(propertyId, checkIn, checkOut, 0, 0, List.of());
        }

        List<Bed> allBeds = bedRepository.findByRoomPropertyId(propertyId);
        Set<UUID> bookedBedIds = bookingRepository.findOverlappingBedIdsForProperty(
                propertyId, checkIn, checkOut, Instant.now()
        );
        Set<UUID> blockedBedIds = blockRepository.findBlockedBedIdsForProperty(propertyId, checkIn, checkOut);

        PropertyPricing propPricing = property.getPricing();

        List<AvailableBedInfo> availableBeds = allBeds.stream()
                .filter(b -> b.getStatus() == BedStatus.AVAILABLE)
                .filter(b -> b.getRoom().getStatus() == RoomStatus.AVAILABLE)
                .filter(b -> !bookedBedIds.contains(b.getId()) && !blockedBedIds.contains(b.getId()))
                .map(b -> {
                    Room r = b.getRoom();
                    return new AvailableBedInfo(
                            b.getId(),
                            b.getBedNumber(),
                            r.getId(),
                            r.getRoomNumber(),
                            r.getRoomType(),
                            r.getFloor(),
                            r.getHourlyRate() != null ? r.getHourlyRate() : (propPricing != null ? propPricing.getHourlyRate() : null),
                            r.getNightlyRate() != null ? r.getNightlyRate() : (propPricing != null ? propPricing.getNightlyRate() : null),
                            r.getWeeklyRate() != null ? r.getWeeklyRate() : (propPricing != null ? propPricing.getWeeklyRate() : null),
                            r.getMonthlyRate() != null ? r.getMonthlyRate() : (propPricing != null ? propPricing.getMonthlyRate() : null)
                    );
                })
                .toList();

        return new PropertyAvailabilityResponse(
                propertyId,
                checkIn,
                checkOut,
                allBeds.size(),
                availableBeds.size(),
                availableBeds
        );
    }

    @Transactional(readOnly = true)
    public boolean isBedAvailable(UUID bedId, Instant checkIn, Instant checkOut) {
        validateTimeWindow(checkIn, checkOut);
        return !bookingRepository.existsOverlappingBooking(bedId, checkIn, checkOut, Instant.now())
                && !blockRepository.existsOverlappingBlock(bedId, checkIn, checkOut);
    }

    public void validateTimeWindow(Instant checkIn, Instant checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BadRequestException("Check-in and check-out times are required", "INVALID_DATES");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("Check-out must be strictly after check-in", "INVALID_DATES");
        }
    }
}
