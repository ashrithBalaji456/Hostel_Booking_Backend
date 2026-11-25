package com.accommodation.platform.availability.service;

import com.accommodation.platform.availability.repository.AvailabilityBlockRepository;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.enums.RoomStatus;
import com.accommodation.platform.common.enums.RoomType;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.room.entity.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityBlockRepository blockRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void filtersOutBookedAndMaintenanceBeds() {
        UUID propertyId = UUID.randomUUID();
        Property property = new Property();
        property.setId(propertyId);
        property.setVerificationStatus(VerificationStatus.APPROVED);
        property.setListingStatus(ListingStatus.ACTIVE);
        PropertyPricing pricing = new PropertyPricing();
        pricing.setNightlyRate(new BigDecimal("450.00"));
        property.setPricing(pricing);

        Room room = new Room();
        room.setProperty(property);
        room.setRoomNumber("101");
        room.setRoomType(RoomType.DORMITORY);
        room.setStatus(RoomStatus.AVAILABLE);

        Bed bed1 = org.mockito.Mockito.mock(Bed.class);
        UUID bed1Id = UUID.randomUUID();
        when(bed1.getId()).thenReturn(bed1Id);
        when(bed1.getBedNumber()).thenReturn("101-B1");
        when(bed1.getStatus()).thenReturn(BedStatus.AVAILABLE);
        when(bed1.getRoom()).thenReturn(room);

        Bed bed2 = org.mockito.Mockito.mock(Bed.class);
        UUID bed2Id = UUID.randomUUID();
        when(bed2.getId()).thenReturn(bed2Id);
        when(bed2.getStatus()).thenReturn(BedStatus.AVAILABLE);
        when(bed2.getRoom()).thenReturn(room);

        Bed bed3 = org.mockito.Mockito.mock(Bed.class);
        when(bed3.getStatus()).thenReturn(BedStatus.MAINTENANCE);

        Instant checkIn = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant checkOut = checkIn.plus(2, ChronoUnit.DAYS);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(bedRepository.findByRoomPropertyId(propertyId)).thenReturn(List.of(bed1, bed2, bed3));
        when(bookingRepository.findOverlappingBedIdsForProperty(eq(propertyId), eq(checkIn), eq(checkOut), any()))
                .thenReturn(Set.of(bed2Id));

        var response = availabilityService.checkPropertyAvailability(propertyId, checkIn, checkOut);

        assertEquals(3, response.totalBeds());
        assertEquals(1, response.availableBedsCount());
        assertEquals(bed1Id, response.availableBeds().get(0).bedId());
    }

    @Test
    void rejectsInvalidDateRange() {
        Instant now = Instant.now();
        assertThrows(BadRequestException.class, () -> availabilityService.validateTimeWindow(now, now));
        assertThrows(BadRequestException.class, () -> availabilityService.validateTimeWindow(now.plusSeconds(60), now));
    }
}
