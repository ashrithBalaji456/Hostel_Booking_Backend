package com.accommodation.platform.property.controller;

import com.accommodation.platform.availability.dto.AvailabilityDtos.PropertyAvailabilityResponse;
import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.common.enums.GenderPolicy;
import com.accommodation.platform.common.enums.PropertyType;
import com.accommodation.platform.common.enums.RoomType;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.property.dto.PropertyDtos.PropertyDetailResponse;
import com.accommodation.platform.property.dto.PropertyDtos.PropertySearchItemResponse;
import com.accommodation.platform.property.service.PropertySearchService;
import com.accommodation.platform.property.service.PropertyService;
import com.accommodation.platform.property.service.SpecialSearchService;
import com.accommodation.platform.property.service.SpecialSearchService.InterviewModeResult;
import com.accommodation.platform.review.dto.ReviewDtos.ReviewResponse;
import com.accommodation.platform.review.service.ReviewService;
import com.accommodation.platform.room.dto.RoomDtos.RoomResponse;
import com.accommodation.platform.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Properties (Public)", description = "Search properties, view details, rooms, availability, and reviews")
public class PropertyController {

        private final PropertySearchService searchService;
        private final PropertyService propertyService;
        private final AvailabilityService availabilityService;
        private final RoomService roomService;
        private final ReviewService reviewService;
        private final SpecialSearchService specialSearchService;

        public PropertyController(
                        PropertySearchService searchService,
                        PropertyService propertyService,
                        AvailabilityService availabilityService,
                        RoomService roomService,
                        ReviewService reviewService,
                        SpecialSearchService specialSearchService) {
                this.searchService = searchService;
                this.propertyService = propertyService;
                this.availabilityService = availabilityService;
                this.roomService = roomService;
                this.reviewService = reviewService;
                this.specialSearchService = specialSearchService;
        }

        @GetMapping
        @Operation(summary = "Search and filter approved properties")
        public ResponseEntity<ApiResponse<PagedResponse<PropertySearchItemResponse>>> searchProperties(
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) String locality,
                        @RequestParam(required = false) String pincode,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude,
                        @RequestParam(required = false) Double radiusKm,
                        @RequestParam(required = false) Instant checkIn,
                        @RequestParam(required = false) Instant checkOut,
                        @RequestParam(required = false) StayType stayType,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) PropertyType propertyType,
                        @RequestParam(required = false) RoomType roomType,
                        @RequestParam(required = false) GenderPolicy genderPolicy,
                        @RequestParam(required = false) List<String> amenities,
                        @RequestParam(required = false) Double minRating,
                        @RequestParam(required = false, defaultValue = "true") Boolean verifiedOnly,
                        @RequestParam(required = false, defaultValue = "false") Boolean availableOnly,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Properties fetched successfully",
                                searchService.search(city, locality, pincode, latitude, longitude, radiusKm,
                                                checkIn, checkOut, stayType, minPrice, maxPrice, propertyType,
                                                roomType, genderPolicy, amenities, minRating, verifiedOnly,
                                                availableOnly, sortBy, page, size)));
        }

        @GetMapping("/{propertyId}")
        @Operation(summary = "Get approved property details")
        public ResponseEntity<ApiResponse<PropertyDetailResponse>> getProperty(@PathVariable UUID propertyId) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Property details fetched",
                                propertyService.getPropertyDetails(propertyId, false)));
        }

        @GetMapping("/{propertyId}/availability")
        @Operation(summary = "Check available beds for a property in a given time window")
        public ResponseEntity<ApiResponse<PropertyAvailabilityResponse>> getAvailability(
                        @PathVariable UUID propertyId,
                        @RequestParam Instant checkIn,
                        @RequestParam Instant checkOut) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Availability fetched",
                                availabilityService.checkPropertyAvailability(propertyId, checkIn, checkOut)));
        }

        @GetMapping("/{propertyId}/rooms")
        @Operation(summary = "List rooms and beds in a property")
        public ResponseEntity<ApiResponse<List<RoomResponse>>> getRooms(@PathVariable UUID propertyId) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Rooms fetched",
                                roomService.getRoomsByProperty(propertyId)));
        }

        @GetMapping("/{propertyId}/reviews")
        @Operation(summary = "List reviews for a property")
        public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviews(
                        @PathVariable UUID propertyId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Reviews fetched",
                                reviewService.getPropertyReviews(propertyId, page, size)));
        }

        @GetMapping("/emergency")
        @Operation(summary = "Search properties supporting same-day or late-night emergency stays")
        public ResponseEntity<ApiResponse<PagedResponse<PropertySearchItemResponse>>> searchEmergency(
                        @RequestParam(required = false) String city,
                        @RequestParam(required = false) Double latitude,
                        @RequestParam(required = false) Double longitude,
                        @RequestParam(required = false, defaultValue = "15.0") Double radiusKm,
                        @RequestParam Instant checkIn,
                        @RequestParam Instant checkOut,
                        @RequestParam(required = false) BigDecimal maxBudget,
                        @RequestParam(required = false) GenderPolicy genderPolicy,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Emergency stay properties fetched",
                                specialSearchService.searchEmergencyStays(
                                                city, latitude, longitude, radiusKm, checkIn, checkOut, maxBudget,
                                                genderPolicy, page, size)));
        }

        @GetMapping("/interview-mode")
        @Operation(summary = "Search accommodation near an interview location with travel time estimates")
        public ResponseEntity<ApiResponse<List<InterviewModeResult>>> searchInterviewMode(
                        @RequestParam double interviewLat,
                        @RequestParam double interviewLon,
                        @RequestParam(defaultValue = "10.0") double radiusKm,
                        @RequestParam Instant checkIn,
                        @RequestParam Instant checkOut,
                        @RequestParam(required = false) BigDecimal maxBudget,
                        @RequestParam(required = false) GenderPolicy genderPolicy) {
                return ResponseEntity.ok(ApiResponse.ok(
                                "Interview mode accommodations fetched",
                                specialSearchService.searchInterviewMode(
                                                interviewLat, interviewLon, radiusKm, checkIn, checkOut, maxBudget,
                                                genderPolicy)));
        }
}
