package com.accommodation.platform.room.service;

import com.accommodation.platform.bed.dto.BedDtos.BedResponse;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.enums.RoomStatus;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.room.dto.RoomDtos.CreateRoomRequest;
import com.accommodation.platform.room.dto.RoomDtos.RoomResponse;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PropertyRepository propertyRepository;

    public RoomService(
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PropertyRepository propertyRepository
    ) {
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional
    public RoomResponse createRoom(UUID ownerId, UUID propertyId, CreateRoomRequest request) {
        Property property = propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by user", "PROPERTY_NOT_FOUND"));

        if (roomRepository.existsByPropertyIdAndRoomNumberIgnoreCase(propertyId, request.roomNumber().trim())) {
            throw new ConflictException("Room number already exists in this property", "DUPLICATE_ROOM");
        }

        Room room = new Room();
        room.setProperty(property);
        room.setRoomNumber(request.roomNumber().trim());
        room.setRoomType(request.roomType());
        room.setFloor(request.floor());
        room.setCapacity(request.capacity());
        room.setAmenities(request.amenities());
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHourlyRate(request.hourlyRate());
        room.setNightlyRate(request.nightlyRate());
        room.setWeeklyRate(request.weeklyRate());
        room.setMonthlyRate(request.monthlyRate());
        room = roomRepository.save(room);

        List<Bed> createdBeds = new ArrayList<>();
        if (request.autoCreateBeds()) {
            for (int i = 1; i <= request.capacity(); i++) {
                Bed bed = new Bed();
                bed.setRoom(room);
                bed.setBedNumber(room.getRoomNumber() + "-B" + i);
                bed.setStatus(BedStatus.AVAILABLE);
                createdBeds.add(bedRepository.save(bed));
            }
        }

        return toResponse(room, createdBeds);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByProperty(UUID propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND");
        }
        List<Room> rooms = roomRepository.findByPropertyIdOrderByFloorAscRoomNumberAsc(propertyId);
        return rooms.stream()
                .map(room -> toResponse(room, bedRepository.findByRoomIdOrderByBedNumberAsc(room.getId())))
                .toList();
    }

    public RoomResponse toResponse(Room room, List<Bed> beds) {
        List<BedResponse> bedResponses = beds.stream()
                .map(b -> new BedResponse(b.getId(), room.getId(), b.getBedNumber(), b.getStatus()))
                .toList();
        return new RoomResponse(
                room.getId(),
                room.getProperty().getId(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getFloor(),
                room.getCapacity(),
                room.getAmenities(),
                room.getStatus(),
                room.getHourlyRate(),
                room.getNightlyRate(),
                room.getWeeklyRate(),
                room.getMonthlyRate(),
                bedResponses
        );
    }
}
