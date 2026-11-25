package com.accommodation.platform.favorite.service;

import com.accommodation.platform.common.enums.PropertyType;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.favorite.entity.Favorite;
import com.accommodation.platform.favorite.repository.FavoriteRepository;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            PropertyRepository propertyRepository
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    @Transactional
    public FavoriteResponse addFavorite(UUID userId, UUID propertyId) {
        if (favoriteRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new ConflictException("Property is already in favorites", "DUPLICATE_FAVORITE");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProperty(property);
        favorite = favoriteRepository.save(favorite);

        return toResponse(favorite);
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID propertyId) {
        Favorite favorite = favoriteRepository.findByUserIdAndPropertyId(userId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found", "FAVORITE_NOT_FOUND"));
        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FavoriteResponse> getUserFavorites(UUID userId, int page, int size) {
        return PagedResponse.from(
                favoriteRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
                ).map(this::toResponse)
        );
    }

    private FavoriteResponse toResponse(Favorite f) {
        Property p = f.getProperty();
        return new FavoriteResponse(
                f.getId(),
                p.getId(),
                p.getName(),
                p.getPropertyType(),
                p.getCity(),
                p.getLocality(),
                f.getCreatedAt()
        );
    }

    public record FavoriteResponse(
            UUID id,
            UUID propertyId,
            String propertyName,
            PropertyType propertyType,
            String city,
            String locality,
            Instant createdAt
    ) {}
}
