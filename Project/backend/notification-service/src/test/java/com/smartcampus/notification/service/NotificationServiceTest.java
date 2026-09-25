package com.smartcampus.notification.service;

import com.smartcampus.notification.client.AuthServiceClient;
import com.smartcampus.notification.client.dto.UserValidationResponseDto;
import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.dto.response.NotificationResponse;
import com.smartcampus.notification.dto.response.UnreadCountResponse;
import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.entity.NotificationStatus;
import com.smartcampus.notification.entity.NotificationType;
import com.smartcampus.notification.exception.BadRequestException;
import com.smartcampus.notification.exception.ResourceNotFoundException;
import com.smartcampus.notification.repository.NotificationRepository;
import com.smartcampus.notification.security.UserPrincipal;
import com.smartcampus.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;
    private UserPrincipal studentPrincipal;
    private UserPrincipal otherStudentPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        notification = new Notification(
                101L,
                "Grade Published",
                "Your CS-301 Midterm grade is now available.",
                NotificationType.ACADEMIC,
                "ACADEMIC_RECORD",
                55L
        );
        notification.setId(1L);

        studentPrincipal = new UserPrincipal(101L, "student1", "student1@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        otherStudentPrincipal = new UserPrincipal(102L, "student2", "student2@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Create notification - Success with recipient validation")
    void createNotification_Success() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                101L,
                "Grade Published",
                "Your CS-301 Midterm grade is now available.",
                NotificationType.ACADEMIC,
                "ACADEMIC_RECORD",
                55L
        );

        when(authServiceClient.validateUser(eq(101L), isNull(), any())).thenReturn(new UserValidationResponseDto(101L, true, true));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createNotification(request, "Bearer test-token");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(101L, response.getRecipientUserId());
        assertEquals("Grade Published", response.getTitle());
        assertEquals(NotificationStatus.UNREAD, response.getStatus());
        verify(authServiceClient).validateUser(eq(101L), isNull(), any());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Create notification - Invalid recipient ID throws BadRequestException")
    void createNotification_InvalidRecipient() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                -5L, "Title", "Message", NotificationType.GENERAL, null, null
        );

        assertThrows(BadRequestException.class, () -> notificationService.createNotification(request, null));
    }

    @Test
    @DisplayName("Get my notifications - Success")
    void getMyNotifications_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByRecipientUserId(101L, pageable)).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getMyNotifications(studentPrincipal, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Grade Published", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Get notification by ID - Owner access success")
    void getNotificationById_Owner_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(1L, studentPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Get notification by ID - Non-owner access throws AccessDeniedException")
    void getNotificationById_NonOwner_ThrowsForbidden() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> notificationService.getNotificationById(1L, otherStudentPrincipal));
    }

    @Test
    @DisplayName("Get notification by ID - Admin access success")
    void getNotificationById_Admin_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(1L, adminPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Get notification by ID - Not found throws ResourceNotFoundException")
    void getNotificationById_NotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.getNotificationById(999L, studentPrincipal));
    }

    @Test
    @DisplayName("Mark as read - Owner marks as READ")
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(1L, studentPrincipal);

        assertNotNull(response);
        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
    }

    @Test
    @DisplayName("Mark as read - Non-owner throws AccessDeniedException")
    void markAsRead_NonOwner_ThrowsForbidden() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> notificationService.markAsRead(1L, otherStudentPrincipal));
    }

    @Test
    @DisplayName("Mark as unread - Owner marks as UNREAD")
    void markAsUnread_Success() {
        notification.setStatus(NotificationStatus.READ);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsUnread(1L, studentPrincipal);

        assertNotNull(response);
        assertEquals(NotificationStatus.UNREAD, notification.getStatus());
        assertNull(notification.getReadAt());
    }

    @Test
    @DisplayName("Delete notification - Owner deletes successfully")
    void deleteNotification_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, studentPrincipal);

        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("Delete notification - Non-owner throws AccessDeniedException")
    void deleteNotification_NonOwner_ThrowsForbidden() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> notificationService.deleteNotification(1L, otherStudentPrincipal));
    }

    @Test
    @DisplayName("Get unread count - Success")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientUserIdAndStatus(101L, NotificationStatus.UNREAD)).thenReturn(3L);

        UnreadCountResponse response = notificationService.getUnreadCount(studentPrincipal);

        assertNotNull(response);
        assertEquals(3L, response.getCount());
    }
}
