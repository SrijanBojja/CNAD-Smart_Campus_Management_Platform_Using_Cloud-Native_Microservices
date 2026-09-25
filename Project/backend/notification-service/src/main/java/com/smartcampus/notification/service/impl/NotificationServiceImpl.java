package com.smartcampus.notification.service.impl;

import com.smartcampus.notification.client.AuthServiceClient;
import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.dto.response.NotificationResponse;
import com.smartcampus.notification.dto.response.UnreadCountResponse;
import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.entity.NotificationStatus;
import com.smartcampus.notification.exception.BadRequestException;
import com.smartcampus.notification.exception.ResourceNotFoundException;
import com.smartcampus.notification.repository.NotificationRepository;
import com.smartcampus.notification.security.UserPrincipal;
import com.smartcampus.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final AuthServiceClient authServiceClient;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   AuthServiceClient authServiceClient) {
        this.notificationRepository = notificationRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UserPrincipal principal, Pageable pageable) {
        log.info("Fetching notifications for user ID: {}, pageable: {}", principal.getUserId(), pageable);
        return notificationRepository.findByRecipientUserId(principal.getUserId(), pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id, UserPrincipal principal) {
        log.info("Fetching notification ID: {} by user ID: {}", id, principal.getUserId());
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!isAdmin(principal) && !notification.getRecipientUserId().equals(principal.getUserId())) {
            log.warn("Access denied: User {} attempted to access notification {} belonging to user {}",
                    principal.getUserId(), id, notification.getRecipientUserId());
            throw new AccessDeniedException("You are not authorized to view this notification");
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request, String bearerToken) {
        log.info("Creating notification for recipient ID: {}, type: {}", request.getRecipientUserId(), request.getNotificationType());

        if (request.getRecipientUserId() == null || request.getRecipientUserId() <= 0) {
            throw new BadRequestException("Valid recipient user ID is required");
        }

        // Validate recipient existence via Auth Service
        authServiceClient.validateUser(request.getRecipientUserId(), null, bearerToken);

        Notification notification = new Notification(
                request.getRecipientUserId(),
                request.getTitle().trim(),
                request.getMessage().trim(),
                request.getNotificationType(),
                request.getReferenceType() != null ? request.getReferenceType().trim() : null,
                request.getReferenceId()
        );

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created successfully with ID: {}", saved.getId());
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    public NotificationResponse markAsRead(Long id, UserPrincipal principal) {
        log.info("Marking notification ID: {} as READ by user ID: {}", id, principal.getUserId());
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!isAdmin(principal) && !notification.getRecipientUserId().equals(principal.getUserId())) {
            log.warn("Access denied: User {} attempted to mark as read notification {} belonging to user {}",
                    principal.getUserId(), id, notification.getRecipientUserId());
            throw new AccessDeniedException("You are not authorized to update this notification");
        }

        if (notification.getStatus() != NotificationStatus.READ) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    public NotificationResponse markAsUnread(Long id, UserPrincipal principal) {
        log.info("Marking notification ID: {} as UNREAD by user ID: {}", id, principal.getUserId());
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!isAdmin(principal) && !notification.getRecipientUserId().equals(principal.getUserId())) {
            log.warn("Access denied: User {} attempted to mark as unread notification {} belonging to user {}",
                    principal.getUserId(), id, notification.getRecipientUserId());
            throw new AccessDeniedException("You are not authorized to update this notification");
        }

        if (notification.getStatus() != NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.UNREAD);
            notification.setReadAt(null);
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    public void deleteNotification(Long id, UserPrincipal principal) {
        log.info("Deleting notification ID: {} by user ID: {}", id, principal.getUserId());
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!isAdmin(principal) && !notification.getRecipientUserId().equals(principal.getUserId())) {
            log.warn("Access denied: User {} attempted to delete notification {} belonging to user {}",
                    principal.getUserId(), id, notification.getRecipientUserId());
            throw new AccessDeniedException("You are not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
        log.info("Notification ID: {} deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UserPrincipal principal) {
        log.info("Fetching unread notification count for user ID: {}", principal.getUserId());
        long count = notificationRepository.countByRecipientUserIdAndStatus(principal.getUserId(), NotificationStatus.UNREAD);
        return new UnreadCountResponse(count);
    }

    private boolean isAdmin(UserPrincipal principal) {
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        return principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
