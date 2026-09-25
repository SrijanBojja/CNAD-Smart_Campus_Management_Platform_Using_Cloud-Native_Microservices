package com.smartcampus.notification.dto.response;

import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.entity.NotificationStatus;
import com.smartcampus.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response representing notification details")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Recipient user ID", example = "101")
    private Long recipientUserId;

    @Schema(description = "Title", example = "Classroom Relocation Notice")
    private String title;

    @Schema(description = "Message body", example = "CS-301 lecture tomorrow moved to Main Auditorium Hall A.")
    private String message;

    @Schema(description = "Notification category", example = "ACADEMIC")
    private NotificationType notificationType;

    @Schema(description = "Referenced entity type", example = "FACILITY_REQUEST")
    private String referenceType;

    @Schema(description = "Referenced entity ID", example = "25")
    private Long referenceId;

    @Schema(description = "Read status", example = "UNREAD")
    private NotificationStatus status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Read timestamp if read, null otherwise")
    private LocalDateTime readAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public NotificationResponse() {
    }

    public static NotificationResponse fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setRecipientUserId(notification.getRecipientUserId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setNotificationType(notification.getNotificationType());
        response.setReferenceType(notification.getReferenceType());
        response.setReferenceId(notification.getReferenceId());
        response.setStatus(notification.getStatus());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
