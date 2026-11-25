package com.accommodation.platform.review.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.review.dto.ReviewDtos.*;
import com.accommodation.platform.review.entity.Review;
import com.accommodation.platform.review.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public ReviewResponse createReview(UUID userId, CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only review your own bookings");
        }
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed stays can be reviewed", "REVIEW_NOT_ELIGIBLE");
        }
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new ConflictException("A review has already been submitted for this booking", "DUPLICATE_REVIEW");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(booking.getUser());
        review.setProperty(booking.getProperty());
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(UUID userId, UUID reviewId, UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found", "REVIEW_NOT_FOUND"));
        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own reviews");
        }
        review.setRating(request.rating());
        review.setComment(request.comment());
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(UUID actorId, UserRole actorRole, UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found", "REVIEW_NOT_FOUND"));
        if (actorRole != UserRole.ADMIN && !review.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("You do not have permission to delete this review");
        }
        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getPropertyReviews(UUID propertyId, int page, int size) {
        return PagedResponse.from(
                reviewRepository.findByPropertyIdAndHiddenFalseOrderByCreatedAtDesc(
                        propertyId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
                ).map(this::toResponse)
        );
    }

    public ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getBooking().getId(),
                r.getProperty().getId(),
                r.getUser().getId(),
                r.getUser().getFullName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
