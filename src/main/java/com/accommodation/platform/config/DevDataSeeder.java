package com.accommodation.platform.config;

import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.property.entity.Amenity;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.property.repository.AmenityRepository;
import com.accommodation.platform.property.repository.PropertyPricingRepository;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.room.repository.RoomRepository;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyPricingRepository pricingRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            PropertyPricingRepository pricingRepository,
            AmenityRepository amenityRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.pricingRepository = pricingRepository;
        this.amenityRepository = amenityRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureUser("Platform Admin", "admin@spatialstay.in", "Admin@123", "9876500001", UserRole.ADMIN, Gender.OTHER);
        User owner = ensureUser("Rohan Verma (Owner)", "owner@spatialstay.in", "Owner@123", "9876500002", UserRole.PROPERTY_OWNER, Gender.MALE);
        ensureUser("Ananya Student", "student@spatialstay.in", "Student@123", "9876500003", UserRole.CUSTOMER, Gender.FEMALE);

        if (propertyRepository.count() == 0) {
            Property p = new Property();
            p.setOwner(owner);
            p.setName("Spatial Stay Indiranagar Hub");
            p.setDescription("Verified hostel and PG near Indiranagar Metro for students, job seekers, and working professionals.");
            p.setPropertyType(PropertyType.HOSTEL);
            p.setAddress("12, 100 Feet Road, Indiranagar");
            p.setLocality("Indiranagar");
            p.setCity("Bengaluru");
            p.setState("Karnataka");
            p.setPincode("560038");
            p.setLatitude(12.9716);
            p.setLongitude(77.5946);
            p.setGenderPolicy(GenderPolicy.CO_ED);
            p.setCheckInTime(LocalTime.of(12, 0));
            p.setCheckOutTime(LocalTime.of(11, 0));
            p.setVerificationStatus(VerificationStatus.APPROVED);
            p.setListingStatus(ListingStatus.ACTIVE);
            p.setInstantBookingEnabled(true);
            p.setEmergencyBookingEnabled(true);
            p.setLateNightCheckInAllowed(true);
            p.setMinShortStayHours(3);
            p.setMaxShortStayDays(14);
            p.setMinLongStayDays(7);
            p.setMaxLongStayMonths(12);
            List<Amenity> amenities = amenityRepository.findByCodeIn(List.of("WIFI", "AC", "LOCKER", "STUDY_ROOM"));
            p.setAmenities(new HashSet<>(amenities));
            p = propertyRepository.save(p);

            PropertyPricing pricing = new PropertyPricing();
            pricing.setProperty(p);
            pricing.setHourlyRate(new BigDecimal("120.00"));
            pricing.setNightlyRate(new BigDecimal("550.00"));
            pricing.setWeeklyRate(new BigDecimal("3200.00"));
            pricing.setMonthlyRate(new BigDecimal("9500.00"));
            pricing.setSecurityDeposit(new BigDecimal("2500.00"));
            pricing.setExtraCharges(BigDecimal.ZERO);
            pricing.setDiscountPercentage(new BigDecimal("5.00"));
            pricingRepository.save(pricing);

            Room room = new Room();
            room.setProperty(p);
            room.setRoomNumber("101");
            room.setRoomType(RoomType.DOUBLE);
            room.setFloor(1);
            room.setCapacity(4);
            room.setAmenities("AC, Personal Locker, Study Desk");
            room.setStatus(RoomStatus.AVAILABLE);
            room = roomRepository.save(room);

            for (int i = 1; i <= 4; i++) {
                Bed bed = new Bed();
                bed.setRoom(room);
                bed.setBedNumber("101-B" + i);
                bed.setStatus(BedStatus.AVAILABLE);
                bedRepository.save(bed);
            }
            log.info("Seeded dev property '{}' with 4 beds (admin={})", p.getName(), admin.getEmail());
        }
    }

    private User ensureUser(String fullName, String email, String rawPassword, String phone, UserRole role, Gender gender) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(fullName);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setPhoneNumber(phone);
            u.setRole(role);
            u.setGender(gender);
            u.setEmailVerified(true);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return userRepository.save(u);
        });
    }
}
