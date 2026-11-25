package com.accommodation.platform.availability.controller;

import com.accommodation.platform.availability.service.AvailabilityBlockService;
import com.accommodation.platform.availability.service.AvailabilityBlockService.AvailabilityBlockResponse;
import com.accommodation.platform.availability.service.AvailabilityBlockService.CreateBlockRequest;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/availability-blocks")
@Tag(name = "Availability Calendar", description = "Owner bed availability blocks for maintenance or personal use")
public class AvailabilityController {

    private final AvailabilityBlockService blockService;

    public AvailabilityController(AvailabilityBlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping
    @Operation(summary = "Block a bed for maintenance or personal use")
    public ResponseEntity<ApiResponse<AvailabilityBlockResponse>> createBlock(
            @Valid @RequestBody CreateBlockRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Availability block created",
                        blockService.createBlock(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/bed/{bedId}")
    @Operation(summary = "List availability blocks for a bed")
    public ResponseEntity<ApiResponse<List<AvailabilityBlockResponse>>> getBlocks(@PathVariable UUID bedId) {
        return ResponseEntity.ok(ApiResponse.ok("Blocks fetched", blockService.getBlocksForBed(bedId)));
    }

    @DeleteMapping("/{blockId}")
    @Operation(summary = "Remove an availability block")
    public ResponseEntity<ApiResponse<Void>> deleteBlock(@PathVariable UUID blockId) {
        blockService.deleteBlock(SecurityUtils.getCurrentUserId(), blockId);
        return ResponseEntity.ok(ApiResponse.ok("Availability block removed"));
    }
}
