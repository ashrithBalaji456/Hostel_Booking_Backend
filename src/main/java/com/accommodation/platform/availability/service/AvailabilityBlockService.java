package com.accommodation.platform.availability.service;

import com.accommodation.platform.availability.entity.AvailabilityBlock;
import com.accommodation.platform.availability.repository.AvailabilityBlockRepository;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AvailabilityBlockService {

    private final AvailabilityBlockRepository blockRepository;
    private final BedRepository bedRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public AvailabilityBlockService(
            AvailabilityBlockRepository blockRepository,
            BedRepository bedRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.blockRepository = blockRepository;
        this.bedRepository = bedRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AvailabilityBlockResponse createBlock(UUID ownerId, CreateBlockRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("Block endTime must be after startTime", "INVALID_DATES");
        }
        Bed bed = bedRepository.findByIdForUpdate(req.bedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));
        if (!bed.getRoom().getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this bed's property");
        }
        if (bookingRepository.existsOverlappingBooking(bed.getId(), req.startTime(), req.endTime(), Instant.now())) {
            throw new ConflictException("Cannot block bed: active booking exists in this window", "BOOKING_CONFLICT");
        }
        if (blockRepository.existsOverlappingBlock(bed.getId(), req.startTime(), req.endTime())) {
            throw new ConflictException("Conflicting availability block already exists", "BLOCK_CONFLICT");
        }

        User owner = userRepository.getReferenceById(ownerId);
        AvailabilityBlock block = new AvailabilityBlock();
        block.setBed(bed);
        block.setBlockType(req.blockType().trim().toUpperCase());
        block.setStartTime(req.startTime());
        block.setEndTime(req.endTime());
        block.setReason(req.reason());
        block.setCreatedBy(owner);
        block = blockRepository.save(block);

        return toResponse(block);
    }

    @Transactional
    public void deleteBlock(UUID ownerId, UUID blockId) {
        AvailabilityBlock block = blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Availability block not found", "BLOCK_NOT_FOUND"));
        if (!block.getBed().getRoom().getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this block");
        }
        blockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityBlockResponse> getBlocksForBed(UUID bedId) {
        return blockRepository.findByBedIdOrderByStartTimeAsc(bedId).stream().map(this::toResponse).toList();
    }

    private AvailabilityBlockResponse toResponse(AvailabilityBlock b) {
        return new AvailabilityBlockResponse(
                b.getId(), b.getBed().getId(), b.getBlockType(),
                b.getStartTime(), b.getEndTime(), b.getReason(), b.getCreatedAt()
        );
    }

    public record CreateBlockRequest(
            @NotNull UUID bedId,
            @NotBlank String blockType,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            String reason
    ) {}

    public record AvailabilityBlockResponse(
            UUID id,
            UUID bedId,
            String blockType,
            Instant startTime,
            Instant endTime,
            String reason,
            Instant createdAt
    ) {}
}
