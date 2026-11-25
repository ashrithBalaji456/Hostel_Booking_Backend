package com.accommodation.platform.property.controller;

import com.accommodation.platform.bed.dto.BedDtos.BedResponse;
import com.accommodation.platform.bed.dto.BedDtos.CreateBedRequest;
import com.accommodation.platform.bed.dto.BedDtos.UpdateBedAvailabilityRequest;
import com.accommodation.platform.bed.service.BedService;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.property.dto.PropertyDtos.CreatePropertyRequest;
import com.accommodation.platform.property.dto.PropertyDtos.PropertyDetailResponse;
import com.accommodation.platform.property.dto.PropertyDtos.UpdatePropertyRequest;
import com.accommodation.platform.property.service.PropertyService;
import com.accommodation.platform.room.dto.RoomDtos.CreateRoomRequest;
import com.accommodation.platform.room.dto.RoomDtos.RoomResponse;
import com.accommodation.platform.room.service.RoomService;
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
@RequestMapping("/api/v1/owner/properties")
@Tag(name = "Owner Properties", description = "Property, room, bed, and availability management for property owners")
public class OwnerPropertyController {

    private final PropertyService propertyService;
    private final RoomService roomService;
    private final BedService bedService;

    public OwnerPropertyController(
            PropertyService propertyService,
            RoomService roomService,
            BedService bedService
    ) {
        this.propertyService = propertyService;
        this.roomService = roomService;
        this.bedService = bedService;
    }

    @GetMapping
    @Operation(summary = "List all properties owned by the current owner")
    public ResponseEntity<ApiResponse<List<PropertyDetailResponse>>> listMyProperties() {
        return ResponseEntity.ok(ApiResponse.ok("Owner properties loaded", propertyService.getOwnerProperties(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping
    @Operation(summary = "Create a new property listing (pending admin verification)")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> createProperty(
            @Valid @RequestBody CreatePropertyRequest request
    ) {
        PropertyDetailResponse response = propertyService.createProperty(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property created successfully", response));
    }

    @PutMapping("/{propertyId}")
    @Operation(summary = "Update an owned property listing")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> updateProperty(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        PropertyDetailResponse response = propertyService.updateProperty(SecurityUtils.getCurrentUserId(), propertyId, request);
        return ResponseEntity.ok(ApiResponse.ok("Property updated successfully", response));
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Delete an owned property listing")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(@PathVariable UUID propertyId) {
        propertyService.deleteProperty(SecurityUtils.getCurrentUserId(), propertyId);
        return ResponseEntity.ok(ApiResponse.ok("Property deleted successfully", null));
    }

    @PostMapping("/{propertyId}/rooms")
    @Operation(summary = "Add a room (and optionally beds) to an owned property")
    public ResponseEntity<ApiResponse<RoomResponse>> addRoom(
            @PathVariable UUID propertyId,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        RoomResponse response = roomService.createRoom(SecurityUtils.getCurrentUserId(), propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Room created successfully", response));
    }

    @PostMapping("/{propertyId}/beds")
    @Operation(summary = "Add a bed to a room in an owned property")
    public ResponseEntity<ApiResponse<BedResponse>> addBed(
            @PathVariable UUID propertyId,
            @Valid @RequestBody CreateBedRequest request
    ) {
        BedResponse response = bedService.addBed(SecurityUtils.getCurrentUserId(), propertyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bed added successfully", response));
    }

    @PutMapping("/{propertyId}/availability")
    @Operation(summary = "Update bed availability status for an owned property")
    public ResponseEntity<ApiResponse<BedResponse>> updateAvailability(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpdateBedAvailabilityRequest request
    ) {
        BedResponse response = bedService.updateBedAvailability(SecurityUtils.getCurrentUserId(), propertyId, request);
        return ResponseEntity.ok(ApiResponse.ok("Bed availability updated", response));
    }
}
