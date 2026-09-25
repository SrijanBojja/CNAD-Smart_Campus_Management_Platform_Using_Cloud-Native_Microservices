package com.smartcampus.event.dto.request;

import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request payload for updating an existing campus event")
public class UpdateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Schema(description = "Event title", example = "Cloud-Native Microservices Hackathon (Updated)")
    private String title;

    @Schema(description = "Event description", example = "Updated description for hackathon")
    private String description;

    @NotNull(message = "Event type is required")
    @Schema(description = "Type of the event", example = "TECHNICAL")
    private EventType eventType;

    @NotNull(message = "Start date time is required")
    @Schema(description = "Event start date and time", example = "2026-10-01T09:30:00")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date time is required")
    @Schema(description = "Event end date and time", example = "2026-10-01T17:30:00")
    private LocalDateTime endDateTime;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    @Schema(description = "Physical or virtual location", example = "Auditorium Hall B")
    private String location;

    @Positive(message = "Capacity must be positive when provided")
    @Schema(description = "Maximum attendee capacity", example = "120")
    private Integer capacity;

    @NotNull(message = "Status is required")
    @Schema(description = "Event lifecycle status", example = "UPCOMING")
    private EventStatus status;

    public UpdateEventRequest() {
    }

    public UpdateEventRequest(String title, String description, EventType eventType,
                              LocalDateTime startDateTime, LocalDateTime endDateTime,
                              String location, Integer capacity, EventStatus status) {
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.location = location;
        this.capacity = capacity;
        this.status = status;
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
}
