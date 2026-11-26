package com.accommodation.platform.property.service;

import com.accommodation.platform.availability.repository.AvailabilityBlockRepository;
import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.property.dto.PropertyDtos.PropertySearchItemResponse;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyImage;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.property.repository.PropertyImageRepository;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.review.repository.ReviewRepository;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.room.repository.RoomRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class PropertySearchService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository imageRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final AvailabilityService availabilityService;
    private final AvailabilityBlockRepository blockRepository;

    public PropertySearchService(
            PropertyRepository propertyRepository,
            PropertyImageRepository imageRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            BookingRepository bookingRepository,
            ReviewRepository reviewRepository,
            AvailabilityService availabilityService,
            AvailabilityBlockRepository blockRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.imageRepository = imageRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.availabilityService = availabilityService;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PropertySearchItemResponse> search(
            String city,
            String locality,
            String pincode,
            Double latitude,
            Double longitude,
            Double radiusKm,
            Instant checkIn,
            Instant checkOut,
            StayType stayType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            PropertyType propertyType,
            RoomType roomType,
            GenderPolicy genderPolicy,
            List<String> amenities,
            Double minRating,
            Boolean verifiedOnly,
            Boolean availableOnly,
            String sortBy,
            int page,
            int size
    ) {
        if (checkIn != null || checkOut != null) {
            availabilityService.validateTimeWindow(checkIn, checkOut);
        }

        boolean requireVerified = verifiedOnly == null || verifiedOnly;
        Specification<Property> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("listingStatus"), ListingStatus.ACTIVE));
            if (requireVerified) {
                predicates.add(cb.equal(root.get("verificationStatus"), VerificationStatus.APPROVED));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase()));
            }
            if (locality != null && !locality.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("locality")), "%" + locality.trim().toLowerCase() + "%"));
            }
            if (pincode != null && !pincode.isBlank()) {
                predicates.add(cb.equal(root.get("pincode"), pincode.trim()));
            }
            if (propertyType != null) {
                predicates.add(cb.equal(root.get("propertyType"), propertyType));
            }
            if (genderPolicy != null) {
                predicates.add(cb.equal(root.get("genderPolicy"), genderPolicy));
            }
            if (minPrice != null) {
                Join<Object, Object> pricingJoin = root.join("pricing");
                predicates.add(cb.greaterThanOrEqualTo(pricingJoin.get("nightlyRate"), minPrice));
            }
            if (maxPrice != null) {
                Join<Object, Object> pricingJoin = root.join("pricing");
                predicates.add(cb.lessThanOrEqualTo(pricingJoin.get("nightlyRate"), maxPrice));
            }
            if (amenities != null && !amenities.isEmpty()) {
                query.distinct(true);
                for (String code : amenities) {
                    Join<Object, Object> amenityJoin = root.join("amenities");
                    predicates.add(cb.equal(cb.upper(amenityJoin.get("code")), code.trim().toUpperCase()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Property> candidates = propertyRepository.findAll(spec);
        List<PropertySearchItemResponse> mapped = new ArrayList<>();

        for (Property p : candidates) {
            if (roomType != null) {
                List<Room> rooms = roomRepository.findByPropertyIdOrderByFloorAscRoomNumberAsc(p.getId());
                boolean hasRoomType = rooms.stream().anyMatch(r -> r.getRoomType() == roomType);
                if (!hasRoomType) {
                    continue;
                }
            }

            Double distanceKm = null;
            if (latitude != null && longitude != null) {
                if (p.getLatitude() == null || p.getLongitude() == null) {
                    continue;
                }
                distanceKm = haversineKm(latitude, longitude, p.getLatitude(), p.getLongitude());
                if (radiusKm != null && distanceKm > radiusKm) {
                    continue;
                }
            }

            long availableCount = countAvailableBeds(p, checkIn, checkOut);
            if (Boolean.TRUE.equals(availableOnly) && availableCount == 0) {
                continue;
            }

            Double avgRating = reviewRepository.findAverageRatingByPropertyId(p.getId());
            long reviewCount = reviewRepository.countByPropertyIdAndHiddenFalse(p.getId());
            if (minRating != null && (avgRating == null || avgRating < minRating)) {
                continue;
            }

            PropertyPricing pricing = p.getPricing();
            List<StayType> stayTypes = new ArrayList<>();
            if (pricing != null && (pricing.getHourlyRate() != null || pricing.getNightlyRate() != null)) {
                stayTypes.add(StayType.SHORT_STAY);
            }
            if (pricing != null && (pricing.getWeeklyRate() != null || pricing.getMonthlyRate() != null)) {
                stayTypes.add(StayType.LONG_STAY);
            }
            if (stayType != null && !stayTypes.contains(stayType)) {
                continue;
            }

            List<String> imageUrls = imageRepository.findByPropertyIdOrderByDisplayOrderAsc(p.getId())
                    .stream()
                    .map(PropertyImage::getImageUrl)
                    .toList();
            String mainImage = imageUrls.isEmpty() ? null : imageUrls.get(0);

            BigDecimal startingPrice = determineStartingPrice(pricing, stayType);

            mapped.add(new PropertySearchItemResponse(
                    p.getId(),
                    p.getName(),
                    p.getAddress(),
                    p.getLocality(),
                    p.getCity(),
                    p.getPincode(),
                    p.getPropertyType(),
                    p.getGenderPolicy(),
                    p.getVerificationStatus(),
                    p.getLatitude(),
                    p.getLongitude(),
                    distanceKm,
                    startingPrice,
                    availableCount,
                    avgRating,
                    reviewCount,
                    mainImage,
                    imageUrls,
                    stayTypes
            ));
        }

        sortResults(mapped, sortBy);

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mapped.size());
        List<PropertySearchItemResponse> slice = start >= mapped.size() ? List.of() : mapped.subList(start, end);
        return PagedResponse.from(new PageImpl<>(slice, pageable, mapped.size()));
    }

    private long countAvailableBeds(Property p, Instant checkIn, Instant checkOut) {
        List<Bed> allBeds = bedRepository.findByRoomPropertyId(p.getId());
        if (checkIn == null || checkOut == null) {
            return allBeds.stream()
                    .filter(b -> b.getStatus() == BedStatus.AVAILABLE && b.getRoom().getStatus() == RoomStatus.AVAILABLE)
                    .count();
        }
        Set<UUID> booked = bookingRepository.findOverlappingBedIdsForProperty(p.getId(), checkIn, checkOut, Instant.now());
        Set<UUID> blocked = blockRepository.findBlockedBedIdsForProperty(p.getId(), checkIn, checkOut);
        return allBeds.stream()
                .filter(b -> b.getStatus() == BedStatus.AVAILABLE && b.getRoom().getStatus() == RoomStatus.AVAILABLE)
                .filter(b -> !booked.contains(b.getId()) && !blocked.contains(b.getId()))
                .count();
    }

    private BigDecimal determineStartingPrice(PropertyPricing pricing, StayType stayType) {
        if (pricing == null) return BigDecimal.ZERO;
        if (stayType == StayType.LONG_STAY) {
            if (pricing.getMonthlyRate() != null) return pricing.getMonthlyRate();
            if (pricing.getWeeklyRate() != null) return pricing.getWeeklyRate();
        }
        return pricing.getNightlyRate();
    }

    private void sortResults(List<PropertySearchItemResponse> list, String sortBy) {
        if (sortBy == null) return;
        switch (sortBy.toLowerCase()) {
            case "price_asc" -> list.sort(Comparator.comparing(PropertySearchItemResponse::startingPrice, Comparator.nullsLast(Comparator.naturalOrder())));
            case "price_desc" -> list.sort(Comparator.comparing(PropertySearchItemResponse::startingPrice, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            case "rating" -> list.sort(Comparator.comparing(PropertySearchItemResponse::averageRating, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
            case "distance" -> list.sort(Comparator.comparing(PropertySearchItemResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
            default -> {}
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
}
