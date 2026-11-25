package com.accommodation.platform.property.service;

import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.property.dto.PropertyDtos.*;
import com.accommodation.platform.property.entity.Amenity;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyImage;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.property.repository.AmenityRepository;
import com.accommodation.platform.property.repository.PropertyImageRepository;
import com.accommodation.platform.property.repository.PropertyPricingRepository;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.review.repository.ReviewRepository;
import com.accommodation.platform.room.dto.RoomDtos.RoomResponse;
import com.accommodation.platform.room.service.RoomService;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyPricingRepository pricingRepository;
    private final PropertyImageRepository imageRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final ReviewRepository reviewRepository;

    public PropertyService(
            PropertyRepository propertyRepository,
            PropertyPricingRepository pricingRepository,
            PropertyImageRepository imageRepository,
            AmenityRepository amenityRepository,
            UserRepository userRepository,
            RoomService roomService,
            ReviewRepository reviewRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.pricingRepository = pricingRepository;
        this.imageRepository = imageRepository;
        this.amenityRepository = amenityRepository;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public PropertyDetailResponse createProperty(UUID ownerId, CreatePropertyRequest req) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found", "USER_NOT_FOUND"));

        Property property = new Property();
        property.setOwner(owner);
        property.setName(req.name().trim());
        property.setDescription(req.description());
        property.setPropertyType(req.propertyType());
        property.setAddress(req.address().trim());
        property.setLocality(req.locality());
        property.setCity(req.city().trim());
        property.setState(req.state().trim());
        property.setPincode(req.pincode().trim());
        property.setLatitude(req.latitude());
        property.setLongitude(req.longitude());
        property.setGenderPolicy(req.genderPolicy());
        property.setCheckInTime(req.checkInTime() != null ? req.checkInTime() : LocalTime.of(12, 0));
        property.setCheckOutTime(req.checkOutTime() != null ? req.checkOutTime() : LocalTime.of(11, 0));
        property.setVerificationStatus(VerificationStatus.PENDING);
        property.setListingStatus(ListingStatus.ACTIVE);
        property.setInstantBookingEnabled(req.instantBookingEnabled());
        if (req.minShortStayHours() != null) property.setMinShortStayHours(req.minShortStayHours());
        if (req.maxShortStayDays() != null) property.setMaxShortStayDays(req.maxShortStayDays());
        if (req.minLongStayDays() != null) property.setMinLongStayDays(req.minLongStayDays());
        if (req.maxLongStayMonths() != null) property.setMaxLongStayMonths(req.maxLongStayMonths());

        if (req.amenityCodes() != null && !req.amenityCodes().isEmpty()) {
            Set<String> upper = req.amenityCodes().stream().map(String::toUpperCase).collect(Collectors.toSet());
            List<Amenity> found = amenityRepository.findByCodeIn(upper);
            if (found.size() != upper.size()) {
                throw new BadRequestException("One or more amenity codes are invalid", "INVALID_AMENITY");
            }
            property.setAmenities(new HashSet<>(found));
        }

        property = propertyRepository.save(property);

        PropertyPricing pricing = new PropertyPricing();
        pricing.setProperty(property);
        applyPricing(pricing, req.pricing());
        pricingRepository.save(pricing);
        property.setPricing(pricing);

        if (req.images() != null) {
            for (ImageRequest imgReq : req.images()) {
                PropertyImage img = new PropertyImage();
                img.setProperty(property);
                img.setImageUrl(imgReq.imageUrl().trim());
                img.setImageType(imgReq.imageType());
                img.setDisplayOrder(imgReq.displayOrder());
                imageRepository.save(img);
            }
        }

        return getPropertyDetails(property.getId(), true);
    }

    @Transactional
    public PropertyDetailResponse updateProperty(UUID ownerId, UUID propertyId, UpdatePropertyRequest req) {
        Property property = propertyRepository.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by user", "PROPERTY_NOT_FOUND"));

        property.setName(req.name().trim());
        property.setDescription(req.description());
        property.setPropertyType(req.propertyType());
        property.setAddress(req.address().trim());
        property.setLocality(req.locality());
        property.setCity(req.city().trim());
        property.setState(req.state().trim());
        property.setPincode(req.pincode().trim());
        property.setLatitude(req.latitude());
        property.setLongitude(req.longitude());
        property.setGenderPolicy(req.genderPolicy());
        if (req.checkInTime() != null) property.setCheckInTime(req.checkInTime());
        if (req.checkOutTime() != null) property.setCheckOutTime(req.checkOutTime());
        property.setInstantBookingEnabled(req.instantBookingEnabled());
        if (req.minShortStayHours() != null) property.setMinShortStayHours(req.minShortStayHours());
        if (req.maxShortStayDays() != null) property.setMaxShortStayDays(req.maxShortStayDays());
        if (req.minLongStayDays() != null) property.setMinLongStayDays(req.minLongStayDays());
        if (req.maxLongStayMonths() != null) property.setMaxLongStayMonths(req.maxLongStayMonths());
        if (req.listingStatus() != null && req.listingStatus() != ListingStatus.SUSPENDED) {
            property.setListingStatus(req.listingStatus());
        }

        if (req.amenityCodes() != null) {
            Set<String> upper = req.amenityCodes().stream().map(String::toUpperCase).collect(Collectors.toSet());
            property.setAmenities(new HashSet<>(amenityRepository.findByCodeIn(upper)));
        }

        if (req.pricing() != null) {
            PropertyPricing pricing = pricingRepository.findByPropertyId(propertyId)
                    .orElseGet(() -> {
                        PropertyPricing p = new PropertyPricing();
                        p.setProperty(property);
                        return p;
                    });
            applyPricing(pricing, req.pricing());
            pricingRepository.save(pricing);
        }

        if (req.images() != null) {
            List<PropertyImage> existing = imageRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId);
            imageRepository.deleteAll(existing);
            for (ImageRequest imgReq : req.images()) {
                PropertyImage img = new PropertyImage();
                img.setProperty(property);
                img.setImageUrl(imgReq.imageUrl().trim());
                img.setImageType(imgReq.imageType());
                img.setDisplayOrder(imgReq.displayOrder());
                imageRepository.save(img);
            }
        }

        propertyRepository.save(property);
        return getPropertyDetails(propertyId, true);
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getPropertyDetails(UUID propertyId, boolean includeUnapproved) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));

        if (!includeUnapproved && (property.getVerificationStatus() != VerificationStatus.APPROVED
                || property.getListingStatus() != ListingStatus.ACTIVE)) {
            throw new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND");
        }

        PropertyPricing pricing = property.getPricing();
        PricingResponse pricingRes = pricing == null ? null : new PricingResponse(
                pricing.getHourlyRate(),
                pricing.getNightlyRate(),
                pricing.getWeeklyRate(),
                pricing.getMonthlyRate(),
                pricing.getSecurityDeposit(),
                pricing.getExtraCharges(),
                pricing.getDiscountPercentage()
        );

        List<ImageResponse> images = imageRepository.findByPropertyIdOrderByDisplayOrderAsc(propertyId)
                .stream()
                .map(i -> new ImageResponse(i.getId(), i.getImageUrl(), i.getImageType(), i.getDisplayOrder()))
                .toList();

        List<RoomResponse> rooms = roomService.getRoomsByProperty(propertyId);
        Set<String> amenityCodes = property.getAmenities().stream().map(Amenity::getCode).collect(Collectors.toSet());

        Double avgRating = reviewRepository.findAverageRatingByPropertyId(propertyId);
        long reviewCount = reviewRepository.countByPropertyIdAndHiddenFalse(propertyId);

        return new PropertyDetailResponse(
                property.getId(),
                property.getOwner().getId(),
                property.getOwner().getFullName(),
                property.getName(),
                property.getDescription(),
                property.getPropertyType(),
                property.getAddress(),
                property.getLocality(),
                property.getCity(),
                property.getState(),
                property.getPincode(),
                property.getLatitude(),
                property.getLongitude(),
                property.getGenderPolicy(),
                property.getCheckInTime(),
                property.getCheckOutTime(),
                property.getVerificationStatus(),
                property.getListingStatus(),
                property.isInstantBookingEnabled(),
                property.getMinShortStayHours(),
                property.getMaxShortStayDays(),
                property.getMinLongStayDays(),
                property.getMaxLongStayMonths(),
                property.getRejectionReason(),
                amenityCodes,
                pricingRes,
                images,
                rooms,
                avgRating,
                reviewCount,
                property.getCreatedAt()
        );
    }

    private void applyPricing(PropertyPricing pricing, PricingRequest req) {
        pricing.setHourlyRate(req.hourlyRate());
        pricing.setNightlyRate(req.nightlyRate());
        pricing.setWeeklyRate(req.weeklyRate());
        pricing.setMonthlyRate(req.monthlyRate());
        pricing.setSecurityDeposit(req.securityDeposit() != null ? req.securityDeposit() : BigDecimal.ZERO);
        pricing.setExtraCharges(req.extraCharges() != null ? req.extraCharges() : BigDecimal.ZERO);
        pricing.setDiscountPercentage(req.discountPercentage() != null ? req.discountPercentage() : BigDecimal.ZERO);
    }
}
