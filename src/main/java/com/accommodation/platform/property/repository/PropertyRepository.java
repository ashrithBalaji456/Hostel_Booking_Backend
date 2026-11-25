package com.accommodation.platform.property.repository;

import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {
    Page<Property> findByOwnerId(UUID ownerId, Pageable pageable);
    List<Property> findByOwnerId(UUID ownerId);
    Optional<Property> findByIdAndOwnerId(UUID id, UUID ownerId);
    Page<Property> findByVerificationStatus(VerificationStatus verificationStatus, Pageable pageable);
    Page<Property> findByVerificationStatusAndListingStatus(VerificationStatus v, ListingStatus l, Pageable pageable);
    long countByVerificationStatus(VerificationStatus verificationStatus);
    long countByOwnerId(UUID ownerId);
    boolean existsByOwnerIdAndNameIgnoreCaseAndPincode(UUID ownerId, String name, String pincode);
}
