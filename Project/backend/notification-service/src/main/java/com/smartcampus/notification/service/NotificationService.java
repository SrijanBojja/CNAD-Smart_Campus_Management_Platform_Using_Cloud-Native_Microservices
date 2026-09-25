package com.smartcampus.notification.service;

import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.dto.response.NotificationResponse;
import com.smartcampus.notification.dto.response.UnreadCountResponse;
import com.smartcampus.notification.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Page<NotificationResponse> getMyNotifications(UserPrincipal principal, Pageable pageable);

    NotificationResponse getNotificationById(Long id, UserPrincipal principal);

    NotificationResponse createNotification(CreateNotificationRequest request, String bearerToken);

    NotificationResponse markAsRead(Long id, UserPrincipal principal);

    NotificationResponse markAsUnread(Long id, UserPrincipal principal);

    void deleteNotification(Long id, UserPrincipal principal);

    UnreadCountResponse getUnreadCount(UserPrincipal principal);
}
