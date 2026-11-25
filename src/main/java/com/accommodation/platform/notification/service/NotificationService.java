package com.accommodation.platform.notification.service;

import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.notification.entity.Notification;
import com.accommodation.platform.notification.repository.NotificationRepository;
import com.accommodation.platform.user.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User user, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setReadStatus(false);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, int page, int size) {
        var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
        ).map(n -> new NotificationResponse(
                n.getId(), n.getTitle(), n.getMessage(), n.getNotificationType(), n.isReadStatus(), n.getCreatedAt()
        ));
        return PagedResponse.from(result);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found", "NOTIFICATION_NOT_FOUND"));
        if (!n.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        n.setReadStatus(true);
        notificationRepository.save(n);
    }

    public record NotificationResponse(
            UUID id,
            String title,
            String message,
            NotificationType notificationType,
            boolean readStatus,
            Instant createdAt
    ) {}
}
