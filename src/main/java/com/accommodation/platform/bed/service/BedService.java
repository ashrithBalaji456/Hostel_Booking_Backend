package com.accommodation.platform.bed.service;

import com.accommodation.platform.bed.dto.BedDtos.BedResponse;
import com.accommodation.platform.bed.dto.BedDtos.CreateBedRequest;
import com.accommodation.platform.bed.dto.BedDtos.UpdateBedAvailabilityRequest;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.room.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BedService {

    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;

    public BedService(BedRepository bedRepository, RoomRepository roomRepository) {
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public BedResponse addBed(UUID ownerId, UUID propertyId, CreateBedRequest request) {
        Room room = roomRepository.findByIdAndPropertyId(request.roomId(), propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found in property", "ROOM_NOT_FOUND"));

        if (!room.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (bedRepository.existsByRoomIdAndBedNumberIgnoreCase(room.getId(), request.bedNumber().trim())) {
            throw new ConflictException("Bed number already exists in this room", "DUPLICATE_BED");
        }

        Bed bed = new Bed();
        bed.setRoom(room);
        bed.setBedNumber(request.bedNumber().trim());
        bed.setStatus(BedStatus.AVAILABLE);
        bed = bedRepository.save(bed);

        long totalBeds = bedRepository.countByRoomId(room.getId());
        room.setCapacity((int) totalBeds);
        roomRepository.save(room);

        return new BedResponse(bed.getId(), room.getId(), bed.getBedNumber(), bed.getStatus());
    }

    @Transactional
    public BedResponse updateBedAvailability(UUID ownerId, UUID propertyId, UpdateBedAvailabilityRequest request) {
        Bed bed = bedRepository.findById(request.bedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));

        if (!bed.getRoom().getProperty().getId().equals(propertyId)
                || !bed.getRoom().getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this bed's property");
        }

        bed.setStatus(request.status());
        bed = bedRepository.save(bed);
        return new BedResponse(bed.getId(), bed.getRoom().getId(), bed.getBedNumber(), bed.getStatus());
    }
}
