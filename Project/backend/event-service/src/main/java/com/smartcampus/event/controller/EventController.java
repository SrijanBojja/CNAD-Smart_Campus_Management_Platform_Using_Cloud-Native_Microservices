package com.smartcampus.event.controller;

import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.request.UpdateEventRequest;
import com.smartcampus.event.dto.response.ErrorResponse;
import com.smartcampus.event.dto.response.EventResponse;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Endpoints for scheduling and managing campus events")
@SecurityRequirement(name = "BearerAuth")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "List Events", description = "Retrieve paginated campus events with optional filters. Accessible by ADMIN, FACULTY, and STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Page<EventResponse>> listEvents(
            @RequestParam(name = "eventType", required = false) EventType eventType,
            @RequestParam(name = "status", required = false) EventStatus status,
            @RequestParam(name = "organizerUserId", required = false) Long organizerUserId,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(page = 0, size = 20, sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<EventResponse> page = eventService.listEvents(eventType, status, organizerUserId, fromDate, toDate, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get Event by ID", description = "Retrieve event details by ID. Accessible by ADMIN, FACULTY, and STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<EventResponse> getEventById(
            @Parameter(description = "Event ID", required = true) @PathVariable("id") Long id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Event", description = "Schedule a new campus event. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or date range", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        String bearerToken = servletRequest.getHeader("Authorization");
        EventResponse response = eventService.createEvent(request, principal, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Event", description = "Update an existing event. Restricted to ADMIN or the organizer FACULTY member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to update this event", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateEventRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        String bearerToken = servletRequest.getHeader("Authorization");
        EventResponse response = eventService.updateEvent(id, request, principal, bearerToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete / Cancel Event", description = "Delete or cancel an event. Restricted to ADMIN or the organizer FACULTY member.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event deleted or cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable("id") Long id,
            Authentication authentication) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        eventService.deleteOrCancelEvent(id, principal);
        return ResponseEntity.noContent().build();
    }
}
