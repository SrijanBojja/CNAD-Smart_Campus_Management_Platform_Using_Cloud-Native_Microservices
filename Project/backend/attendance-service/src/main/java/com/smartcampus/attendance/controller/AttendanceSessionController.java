package com.smartcampus.attendance.controller;

import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.response.AttendanceSessionResponse;
import com.smartcampus.attendance.dto.response.ErrorResponse;
import com.smartcampus.attendance.service.AttendanceSessionService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance/sessions")
@Tag(name = "Attendance Sessions", description = "Endpoints for scheduling and managing class attendance sessions")
@SecurityRequirement(name = "BearerAuth")
public class AttendanceSessionController {

    private final AttendanceSessionService sessionService;

    public AttendanceSessionController(AttendanceSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "List Attendance Sessions", description = "Retrieve paginated attendance sessions. Accessible by ADMIN, FACULTY, and STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance sessions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Page<AttendanceSessionResponse>> listSessions(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "facultyUserId", required = false) Long facultyUserId,
            @RequestParam(name = "sessionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam(name = "academicYear", required = false) String academicYear,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "section", required = false) String section,
            @PageableDefault(page = 0, size = 20, sort = "sessionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AttendanceSessionResponse> response = sessionService.listSessions(
                subjectId, facultyUserId, sessionDate, academicYear, semester, section, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Attendance Session by ID", description = "Retrieve attendance session details by ID. Accessible by ADMIN, FACULTY, and STUDENT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance session retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance session not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<AttendanceSessionResponse> getSessionById(
            @Parameter(description = "Attendance session ID", required = true) @PathVariable("id") Long id) {
        AttendanceSessionResponse response = sessionService.getSessionById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Attendance Session", description = "Schedule a new class attendance session. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attendance session created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation failure", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced Subject not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceSessionResponse> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        AttendanceSessionResponse response = sessionService.createSession(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Attendance Session", description = "Update an existing attendance session. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance session updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance session not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceSessionResponse> updateSession(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAttendanceSessionRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        AttendanceSessionResponse response = sessionService.updateSession(id, request, bearerToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Attendance Session", description = "Delete an attendance session and its associated records. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attendance session deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance session not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Void> deleteSession(@PathVariable("id") Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
