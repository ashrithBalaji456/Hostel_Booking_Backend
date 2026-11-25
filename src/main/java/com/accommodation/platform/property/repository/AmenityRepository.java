package com.accommodation.platform.property.repository;

import com.accommodation.platform.property.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    Optional<Amenity> findByCodeIgnoreCase(String code);
    List<Amenity> findByCodeIn(Collection<String> codes);
}
