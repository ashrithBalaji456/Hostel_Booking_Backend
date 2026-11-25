package com.accommodation.platform.property.repository;

import com.accommodation.platform.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
    List<PropertyImage> findByPropertyIdOrderByDisplayOrderAsc(UUID propertyId);
}
