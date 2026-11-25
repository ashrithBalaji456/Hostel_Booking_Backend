package com.accommodation.platform.property.repository;

import com.accommodation.platform.property.entity.PropertyPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PropertyPricingRepository extends JpaRepository<PropertyPricing, UUID> {
    Optional<PropertyPricing> findByPropertyId(UUID propertyId);
}
