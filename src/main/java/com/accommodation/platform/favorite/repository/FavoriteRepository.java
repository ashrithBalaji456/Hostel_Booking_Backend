package com.accommodation.platform.favorite.repository;

import com.accommodation.platform.favorite.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByUserIdAndPropertyId(UUID userId, UUID propertyId);
    Optional<Favorite> findByUserIdAndPropertyId(UUID userId, UUID propertyId);
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
