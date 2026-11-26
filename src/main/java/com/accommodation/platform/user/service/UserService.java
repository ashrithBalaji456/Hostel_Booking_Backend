package com.accommodation.platform.user.service;

import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.user.dto.UserDtos.UpdateProfileRequest;
import com.accommodation.platform.user.dto.UserDtos.UserProfileResponse;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = getUserEntity(userId);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserEntity(userId);
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setGender(request.gender());
        user.setProfileImage(request.profileImage());
        if (request.occupationType() != null) user.setOccupationType(request.occupationType().trim());
        if (request.organizationName() != null) user.setOrganizationName(request.organizationName().trim());
        if (request.courseOrRole() != null) user.setCourseOrRole(request.courseOrRole().trim());
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public User getUserEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));
    }

    public UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getGender(),
                user.getProfileImage(),
                user.getOccupationType(),
                user.getOrganizationName(),
                user.getCourseOrRole(),
                user.isEmailVerified(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }
}
