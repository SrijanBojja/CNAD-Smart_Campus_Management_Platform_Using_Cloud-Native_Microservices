package com.smartcampus.event.dto.response;

import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response object representing an event")
public class EventResponse {

    @Schema(description = "Unique event ID", example = "1")
    private Long id;

    @Schema(description = "Event title", example = "Cloud-Native Microservices Hackathon")
    private String title;

    @Schema(description = "Event description", example = "Hands-on microservices development hackathon")
    private String description;

    @Schema(description = "Event type", example = "TECHNICAL")
    private EventType eventType;

    @Schema(description = "Start date time", example = "2026-10-01T09:00:00")
    private LocalDateTime startDateTime;

    @Schema(description = "End date time", example = "2026-10-01T17:00:00")
    private LocalDateTime endDateTime;

    @Schema(description = "Location", example = "Auditorium Hall A")
    private String location;

    @Schema(description = "Organizer user ID", example = "201")
    private Long organizerUserId;

    @Schema(description = "Attendee capacity limit", example = "100")
    private Integer capacity;

    @Schema(description = "Event status", example = "UPCOMING")
    private EventStatus status;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public EventResponse() {
    }

    public static EventResponse fromEntity(Event entity) {
        if (entity == null) {
            return null;
        }
        EventResponse dto = new EventResponse();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setEventType(entity.getEventType());
        dto.setStartDateTime(entity.getStartDateTime());
        dto.setEndDateTime(entity.getEndDateTime());
        dto.setLocation(entity.getLocation());
        dto.setOrganizerUserId(entity.getOrganizerUserId());
        dto.setCapacity(entity.getCapacity());
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getOrganizerUserId() {
        return organizerUserId;
    }

    public void setOrganizerUserId(Long organizerUserId) {
        this.organizerUserId = organizerUserId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
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
