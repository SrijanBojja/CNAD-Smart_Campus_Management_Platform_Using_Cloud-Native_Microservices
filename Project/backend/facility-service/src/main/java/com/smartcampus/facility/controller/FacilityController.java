package com.smartcampus.facility.controller;

import com.smartcampus.facility.dto.request.CreateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityStatusRequest;
import com.smartcampus.facility.dto.response.FacilityResponse;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.service.FacilityService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facilities")
@Tag(name = "Facilities", description = "Facility management endpoints for campus rooms, labs, auditoriums, and venues")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "List all facilities", description = "Retrieve a paginated list of facilities with optional filters for type, building, and status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facilities retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<FacilityResponse>> getAllFacilities(
            @RequestParam(required = false) FacilityType facilityType,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) FacilityStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<FacilityResponse> facilities = facilityService.getAllFacilities(facilityType, building, status, pageable);
        return ResponseEntity.ok(facilities);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get facility by ID", description = "Retrieve detailed information for a specific facility")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facility details retrieved"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<FacilityResponse> getFacilityById(@PathVariable Long id) {
        FacilityResponse facility = facilityService.getFacilityById(id);
        return ResponseEntity.ok(facility);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create facility", description = "Create a new campus facility")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Facility created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    })
    public ResponseEntity<FacilityResponse> createFacility(@Valid @RequestBody CreateFacilityRequest request) {
        FacilityResponse response = facilityService.createFacility(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update facility", description = "Update details of an existing facility")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facility updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<FacilityResponse> updateFacility(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFacilityRequest request) {
        FacilityResponse response = facilityService.updateFacility(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update facility status", description = "Update availability or maintenance status of a facility")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Facility status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<FacilityResponse> updateFacilityStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFacilityStatusRequest request) {
        FacilityResponse response = facilityService.updateFacilityStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete or retire facility", description = "Safely deletes or marks facility UNAVAILABLE if history exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Facility deleted or retired successfully"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<Void> deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.noContent().build();
    }
}
