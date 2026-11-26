package com.accommodation.platform.property.entity;

import com.accommodation.platform.common.enums.GenderPolicy;
import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.enums.PropertyType;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(length = 150)
    private String locality;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pincode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy", nullable = false, length = 30)
    private GenderPolicy genderPolicy;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime = LocalTime.of(12, 0);

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime = LocalTime.of(11, 0);

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 30)
    private ListingStatus listingStatus = ListingStatus.ACTIVE;

    @Column(name = "instant_booking_enabled", nullable = false)
    private boolean instantBookingEnabled = true;

    @Column(name = "min_short_stay_hours", nullable = false)
    private int minShortStayHours = 3;

    @Column(name = "max_short_stay_days", nullable = false)
    private int maxShortStayDays = 14;

    @Column(name = "min_long_stay_days", nullable = false)
    private int minLongStayDays = 7;

    @Column(name = "max_long_stay_months", nullable = false)
    private int maxLongStayMonths = 12;

    @Column(name = "min_deposit_refund_months", nullable = false)
    private int minDepositRefundMonths = 1;

    @Column(name = "emergency_booking_enabled", nullable = false)
    private boolean emergencyBookingEnabled = false;

    @Column(name = "late_night_check_in_allowed", nullable = false)
    private boolean lateNightCheckInAllowed = false;

    @Column(name = "latest_check_in_time")
    private LocalTime latestCheckInTime;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToMany
    @JoinTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<Amenity> amenities = new HashSet<>();

    @OneToOne(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PropertyPricing pricing;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PropertyType getPropertyType() { return propertyType; }
    public void setPropertyType(PropertyType propertyType) { this.propertyType = propertyType; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public GenderPolicy getGenderPolicy() { return genderPolicy; }
    public void setGenderPolicy(GenderPolicy genderPolicy) { this.genderPolicy = genderPolicy; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public ListingStatus getListingStatus() { return listingStatus; }
    public void setListingStatus(ListingStatus listingStatus) { this.listingStatus = listingStatus; }
    public boolean isInstantBookingEnabled() { return instantBookingEnabled; }
    public void setInstantBookingEnabled(boolean instantBookingEnabled) { this.instantBookingEnabled = instantBookingEnabled; }
    public int getMinShortStayHours() { return minShortStayHours; }
    public void setMinShortStayHours(int minShortStayHours) { this.minShortStayHours = minShortStayHours; }
    public int getMaxShortStayDays() { return maxShortStayDays; }
    public void setMaxShortStayDays(int maxShortStayDays) { this.maxShortStayDays = maxShortStayDays; }
    public int getMinLongStayDays() { return minLongStayDays; }
    public void setMinLongStayDays(int minLongStayDays) { this.minLongStayDays = minLongStayDays; }
    public int getMaxLongStayMonths() { return maxLongStayMonths; }
    public void setMaxLongStayMonths(int maxLongStayMonths) { this.maxLongStayMonths = maxLongStayMonths; }
    public int getMinDepositRefundMonths() { return minDepositRefundMonths; }
    public void setMinDepositRefundMonths(int minDepositRefundMonths) { this.minDepositRefundMonths = minDepositRefundMonths; }
    public boolean isEmergencyBookingEnabled() { return emergencyBookingEnabled; }
    public void setEmergencyBookingEnabled(boolean emergencyBookingEnabled) { this.emergencyBookingEnabled = emergencyBookingEnabled; }
    public boolean isLateNightCheckInAllowed() { return lateNightCheckInAllowed; }
    public void setLateNightCheckInAllowed(boolean lateNightCheckInAllowed) { this.lateNightCheckInAllowed = lateNightCheckInAllowed; }
    public LocalTime getLatestCheckInTime() { return latestCheckInTime; }
    public void setLatestCheckInTime(LocalTime latestCheckInTime) { this.latestCheckInTime = latestCheckInTime; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Set<Amenity> getAmenities() { return amenities; }
    public void setAmenities(Set<Amenity> amenities) { this.amenities = amenities; }
    public PropertyPricing getPricing() { return pricing; }
    public void setPricing(PropertyPricing pricing) { this.pricing = pricing; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
