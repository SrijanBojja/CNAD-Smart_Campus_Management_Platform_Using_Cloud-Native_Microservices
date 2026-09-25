package com.smartcampus.event.dto.response;

import com.smartcampus.event.entity.EventRegistration;
import com.smartcampus.event.entity.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response object representing an event registration")
public class EventRegistrationResponse {

    @Schema(description = "Unique registration ID", example = "10")
    private Long id;

    @Schema(description = "Referenced event ID", example = "1")
    private Long eventId;

    @Schema(description = "Event title", example = "Cloud-Native Microservices Hackathon")
    private String eventTitle;

    @Schema(description = "Registered user ID", example = "101")
    private Long userId;

    @Schema(description = "Registration timestamp")
    private LocalDateTime registeredAt;

    @Schema(description = "Registration status", example = "REGISTERED")
    private RegistrationStatus status;

    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;

    public EventRegistrationResponse() {
    }

    public static EventRegistrationResponse fromEntity(EventRegistration entity) {
        if (entity == null) {
            return null;
        }
        EventRegistrationResponse dto = new EventRegistrationResponse();
        dto.setId(entity.getId());
        dto.setEventId(entity.getEvent() != null ? entity.getEvent().getId() : null);
        dto.setEventTitle(entity.getEvent() != null ? entity.getEvent().getTitle() : null);
        dto.setUserId(entity.getUserId());
        dto.setRegisteredAt(entity.getRegisteredAt());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
