package com.smartcampus.event.controller;

import com.smartcampus.event.dto.response.ErrorResponse;
import com.smartcampus.event.dto.response.EventRegistrationResponse;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/registrations")
@Tag(name = "Event Registrations", description = "Endpoints for registering and managing campus event attendance")
@SecurityRequirement(name = "BearerAuth")
public class EventRegistrationController {

    private final EventRegistrationService registrationService;

    public EventRegistrationController(EventRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Operation(summary = "Register for Event", description = "Register the authenticated user for an event. Accessible by ADMIN, FACULTY, and STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request or capacity reached", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "User already registered for this event", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable("eventId") Long eventId,
            Authentication authentication) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        EventRegistrationResponse response = registrationService.registerForEvent(eventId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List Event Registrations", description = "Retrieve paginated registrations for an event. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registrations retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Page<EventRegistrationResponse>> listRegistrationsForEvent(
            @PathVariable("eventId") Long eventId,
            @RequestParam(name = "status", required = false) RegistrationStatus status,
            @PageableDefault(page = 0, size = 20, sort = "registeredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<EventRegistrationResponse> response = registrationService.listRegistrationsForEvent(eventId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Registration by ID", description = "Retrieve specific registration details. ADMIN/FACULTY or owner STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to view another user's registration", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<EventRegistrationResponse> getRegistrationById(
            @PathVariable("eventId") Long eventId,
            @PathVariable("registrationId") Long registrationId,
            Authentication authentication) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        EventRegistrationResponse response = registrationService.getRegistrationById(eventId, registrationId, principal);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel Registration", description = "Cancel a registration. Allowed for ADMIN, organizer FACULTY, or the registered user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to cancel this registration", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Registration not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{registrationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<EventRegistrationResponse> cancelRegistration(
            @PathVariable("eventId") Long eventId,
            @PathVariable("registrationId") Long registrationId,
            Authentication authentication) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        EventRegistrationResponse response = registrationService.cancelRegistration(eventId, registrationId, principal);
        return ResponseEntity.ok(response);
    }
}
