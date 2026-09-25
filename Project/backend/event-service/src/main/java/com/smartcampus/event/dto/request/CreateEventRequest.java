package com.smartcampus.event.dto.request;

import com.smartcampus.event.entity.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request payload for creating a campus event")
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    @Schema(description = "Event title", example = "Cloud-Native Microservices Hackathon")
    private String title;

    @Schema(description = "Event description", example = "Hands-on microservices development hackathon for campus students")
    private String description;

    @NotNull(message = "Event type is required")
    @Schema(description = "Type of the event", example = "TECHNICAL")
    private EventType eventType;

    @NotNull(message = "Start date time is required")
    @Schema(description = "Event start date and time", example = "2026-10-01T09:00:00")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date time is required")
    @Schema(description = "Event end date and time", example = "2026-10-01T17:00:00")
    private LocalDateTime endDateTime;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    @Schema(description = "Physical or virtual location", example = "Auditorium Hall A")
    private String location;

    @Positive(message = "Organizer user ID must be positive")
    @Schema(description = "Organizer user ID (optional; defaults to the authenticated user ID)", example = "201")
    private Long organizerUserId;

    @Positive(message = "Capacity must be positive when provided")
    @Schema(description = "Maximum attendee capacity", example = "100")
    private Integer capacity;

    public CreateEventRequest() {
    }

    public CreateEventRequest(String title, String description, EventType eventType,
                              LocalDateTime startDateTime, LocalDateTime endDateTime,
                              String location, Long organizerUserId, Integer capacity) {
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.location = location;
        this.organizerUserId = organizerUserId;
        this.capacity = capacity;
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
}
