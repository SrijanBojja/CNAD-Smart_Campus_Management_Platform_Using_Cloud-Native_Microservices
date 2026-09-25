package com.smartcampus.notification.dto.request;

import com.smartcampus.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to create and dispatch a new notification")
public class CreateNotificationRequest {

    @NotNull(message = "Recipient user ID is required")
    @Positive(message = "Recipient user ID must be positive")
    @Schema(description = "Application user ID of the recipient", example = "101")
    private Long recipientUserId;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    @Schema(description = "Brief title/subject of the notification", example = "Classroom Relocation Notice")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    @Schema(description = "Detailed notification message content", example = "CS-301 lecture tomorrow moved to Main Auditorium Hall A.")
    private String message;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Category of notification", example = "ACADEMIC")
    private NotificationType notificationType;

    @Size(max = 50, message = "Reference type must not exceed 50 characters")
    @Schema(description = "Related domain entity type (e.g. EVENT, ATTENDANCE, FACILITY_REQUEST)", example = "FACILITY_REQUEST")
    private String referenceType;

    @Schema(description = "Related domain entity ID", example = "25")
    private Long referenceId;

    public CreateNotificationRequest() {
    }

    public CreateNotificationRequest(Long recipientUserId, String title, String message,
                                     NotificationType notificationType, String referenceType, Long referenceId) {
        this.recipientUserId = recipientUserId;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
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
}
