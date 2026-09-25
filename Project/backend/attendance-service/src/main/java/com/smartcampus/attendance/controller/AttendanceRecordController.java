package com.smartcampus.attendance.controller;

import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.response.AttendanceRecordResponse;
import com.smartcampus.attendance.dto.response.ErrorResponse;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.security.UserPrincipal;
import com.smartcampus.attendance.service.AttendanceRecordService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance/records")
@Tag(name = "Attendance Records", description = "Endpoints for marking and tracking student attendance records")
@SecurityRequirement(name = "BearerAuth")
public class AttendanceRecordController {

    private final AttendanceRecordService recordService;

    public AttendanceRecordController(AttendanceRecordService recordService) {
        this.recordService = recordService;
    }

    @Operation(summary = "List Attendance Records", description = "Retrieve paginated attendance records. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance records retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Page<AttendanceRecordResponse>> listRecords(
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            @RequestParam(name = "studentId", required = false) Long studentId,
            @RequestParam(name = "status", required = false) AttendanceStatus status,
            @PageableDefault(page = 0, size = 20, sort = "markedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<AttendanceRecordResponse> response = recordService.listRecords(sessionId, studentId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Attendance Record by ID", description = "Retrieve attendance record by ID. ADMIN and FACULTY can view any. STUDENT can only view their own.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance record retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Access denied to other student's record", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    public ResponseEntity<AttendanceRecordResponse> getRecordById(
            @Parameter(description = "Attendance record ID", required = true) @PathVariable("id") Long id,
            Authentication authentication,
            HttpServletRequest request) {

        UserPrincipal principal = (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) ? up : null;
        String bearerToken = request.getHeader("Authorization");
        AttendanceRecordResponse response = recordService.getRecordById(id, principal, bearerToken);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create Attendance Record", description = "Record student attendance for a session. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Attendance record created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Session or Student not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate attendance record for student in session", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceRecordResponse> createRecord(
            @Valid @RequestBody CreateAttendanceRecordRequest request,
            HttpServletRequest servletRequest) {
        String bearerToken = servletRequest.getHeader("Authorization");
        AttendanceRecordResponse response = recordService.createRecord(request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update Attendance Record", description = "Update status or remarks for an attendance record. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Attendance record updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<AttendanceRecordResponse> updateRecord(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAttendanceRecordRequest request) {
        AttendanceRecordResponse response = recordService.updateRecord(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Attendance Record", description = "Delete an individual attendance record. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Attendance record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance record not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    public ResponseEntity<Void> deleteRecord(@PathVariable("id") Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
