package com.smartcampus.notification.controller;

import com.smartcampus.notification.dto.request.CreateNotificationRequest;
import com.smartcampus.notification.dto.response.NotificationResponse;
import com.smartcampus.notification.dto.response.UnreadCountResponse;
import com.smartcampus.notification.security.UserPrincipal;
import com.smartcampus.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification management, dispatch, and read-status endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get current user's notifications", description = "Retrieve a paginated list of notifications belonging exclusively to the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT")
    })
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationResponse> notifications = notificationService.getMyNotifications(principal, pageable);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get unread notification count", description = "Retrieve the total number of unread notifications for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        UnreadCountResponse response = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get notification by ID", description = "Retrieve a single notification. Users may only access their own notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another user's notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationResponse response = notificationService.getNotificationById(id, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Create and dispatch notification", description = "Create a notification for a verified campus recipient. Allowed for ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notification created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or recipient user does not exist"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "503", description = "Auth validation service unavailable")
    })
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "Authorization", required = false) String bearerToken) {

        NotificationResponse response = notificationService.createNotification(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Mark notification as READ", description = "Mark a notification as read. Users can only update their own notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as READ"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot modify another user's notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationResponse response = notificationService.markAsRead(id, principal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Mark notification as UNREAD", description = "Mark a notification as unread. Users can only update their own notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as UNREAD"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot modify another user's notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<NotificationResponse> markAsUnread(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        NotificationResponse response = notificationService.markAsUnread(id, principal);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Delete notification", description = "Delete a notification. Users may only delete their own notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot delete another user's notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        notificationService.deleteNotification(id, principal);
        return ResponseEntity.noContent().build();
    }
}
