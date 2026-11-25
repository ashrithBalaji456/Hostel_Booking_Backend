package com.accommodation.platform.user.controller;

import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.favorite.service.FavoriteService;
import com.accommodation.platform.favorite.service.FavoriteService.FavoriteResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.user.dto.UserDtos.UpdateProfileRequest;
import com.accommodation.platform.user.dto.UserDtos.UserProfileResponse;
import com.accommodation.platform.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current user profile, bookings, and favorites")
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;
    private final FavoriteService favoriteService;

    public UserController(
            UserService userService,
            BookingService bookingService,
            FavoriteService favoriteService
    ) {
        this.userService = userService;
        this.bookingService = bookingService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile fetched successfully",
                userService.getProfile(SecurityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile updated successfully",
                userService.updateProfile(SecurityUtils.getCurrentUserId(), request)
        ));
    }

    @GetMapping("/me/bookings")
    @Operation(summary = "Get booking history for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking history fetched",
                bookingService.getMyBookings(SecurityUtils.getCurrentUserId(), page, size)
        ));
    }

    @GetMapping("/me/favorites")
    @Operation(summary = "Get saved favorite properties for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<FavoriteResponse>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Favorites fetched",
                favoriteService.getUserFavorites(SecurityUtils.getCurrentUserId(), page, size)
        ));
    }

    @PostMapping("/me/favorites/{propertyId}")
    @Operation(summary = "Save a property to favorites")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(@PathVariable UUID propertyId) {
        FavoriteResponse res = favoriteService.addFavorite(SecurityUtils.getCurrentUserId(), propertyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property added to favorites", res));
    }

    @DeleteMapping("/me/favorites/{propertyId}")
    @Operation(summary = "Remove a property from favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(@PathVariable UUID propertyId) {
        favoriteService.removeFavorite(SecurityUtils.getCurrentUserId(), propertyId);
        return ResponseEntity.ok(ApiResponse.ok("Property removed from favorites"));
    }
}
