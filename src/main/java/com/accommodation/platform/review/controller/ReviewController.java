package com.accommodation.platform.review.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.review.dto.ReviewDtos.CreateReviewRequest;
import com.accommodation.platform.review.dto.ReviewDtos.ReviewResponse;
import com.accommodation.platform.review.dto.ReviewDtos.UpdateReviewRequest;
import com.accommodation.platform.review.service.ReviewService;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Submit, edit, and delete property reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Submit a review for a completed booking")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Review submitted successfully", response));
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update an existing review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Review updated successfully",
                reviewService.updateReview(SecurityUtils.getCurrentUserId(), reviewId, request)
        ));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        reviewService.deleteReview(principal.getId(), principal.getRole(), reviewId);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted successfully"));
    }
}
