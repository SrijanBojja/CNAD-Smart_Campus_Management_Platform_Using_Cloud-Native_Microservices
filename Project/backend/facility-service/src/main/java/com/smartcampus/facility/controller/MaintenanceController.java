package com.smartcampus.facility.controller;

import com.smartcampus.facility.dto.request.CreateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceStatusRequest;
import com.smartcampus.facility.dto.response.MaintenanceRecordResponse;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.MaintenanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facilities/{facilityId}/maintenance")
@Tag(name = "Facility Maintenance", description = "Facility maintenance reporting and workflow management endpoints")
public class MaintenanceController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Report a facility maintenance issue", description = "Create a new maintenance work order. Reporter ID is derived from authenticated JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Maintenance record reported successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<MaintenanceRecordResponse> createMaintenanceRecord(
            @PathVariable Long facilityId,
            @Valid @RequestBody CreateMaintenanceRecordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        MaintenanceRecordResponse response = maintenanceRecordService.createMaintenanceRecord(facilityId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "List facility maintenance records", description = "Retrieve a paginated list of maintenance records for a facility")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance records retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<Page<MaintenanceRecordResponse>> getMaintenanceRecordsByFacilityId(
            @PathVariable Long facilityId,
            @PageableDefault(size = 20, sort = "reportedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MaintenanceRecordResponse> responses = maintenanceRecordService.getMaintenanceRecordsByFacilityId(facilityId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Get a single maintenance record", description = "Retrieve details for a specific maintenance work order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance record retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Maintenance record or Facility not found")
    })
    public ResponseEntity<MaintenanceRecordResponse> getMaintenanceRecordById(
            @PathVariable Long facilityId,
            @PathVariable Long maintenanceId) {

        MaintenanceRecordResponse response = maintenanceRecordService.getMaintenanceRecordById(facilityId, maintenanceId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Update maintenance record", description = "Update details of an existing maintenance work order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance record updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Maintenance record or Facility not found")
    })
    public ResponseEntity<MaintenanceRecordResponse> updateMaintenanceRecord(
            @PathVariable Long facilityId,
            @PathVariable Long maintenanceId,
            @Valid @RequestBody UpdateMaintenanceRecordRequest request) {

        MaintenanceRecordResponse response = maintenanceRecordService.updateMaintenanceRecord(facilityId, maintenanceId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{maintenanceId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Update maintenance status", description = "Advance or change the maintenance lifecycle status (OPEN, IN_PROGRESS, RESOLVED, CANCELLED)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Maintenance status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Maintenance record or Facility not found")
    })
    public ResponseEntity<MaintenanceRecordResponse> updateMaintenanceStatus(
            @PathVariable Long facilityId,
            @PathVariable Long maintenanceId,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request) {

        MaintenanceRecordResponse response = maintenanceRecordService.updateMaintenanceStatus(facilityId, maintenanceId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{maintenanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Cancel or delete maintenance record", description = "Marks maintenance record CANCELLED to preserve audit trail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Maintenance record cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Maintenance record or Facility not found")
    })
    public ResponseEntity<Void> cancelOrDeleteMaintenanceRecord(
            @PathVariable Long facilityId,
            @PathVariable Long maintenanceId) {

        maintenanceRecordService.cancelOrDeleteMaintenanceRecord(facilityId, maintenanceId);
        return ResponseEntity.noContent().build();
    }
}
