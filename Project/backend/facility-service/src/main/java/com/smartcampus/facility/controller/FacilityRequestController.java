package com.smartcampus.facility.controller;

import com.smartcampus.facility.dto.request.CreateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequestStatusRequest;
import com.smartcampus.facility.dto.response.FacilityRequestResponse;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.FacilityRequestService;
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
@RequestMapping("/api/v1/facilities/{facilityId}/requests")
@Tag(name = "Facility Requests", description = "Facility booking and reservation request endpoints")
public class FacilityRequestController {

    private final FacilityRequestService facilityRequestService;

    public FacilityRequestController(FacilityRequestService facilityRequestService) {
        this.facilityRequestService = facilityRequestService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Submit a facility booking request", description = "Request to book a facility for a specified date and time slot. Requester is derived from authenticated JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or facility unavailable"),
            @ApiResponse(responseCode = "404", description = "Facility not found"),
            @ApiResponse(responseCode = "409", description = "Facility is already booked for the requested time slot")
    })
    public ResponseEntity<FacilityRequestResponse> createRequest(
            @PathVariable Long facilityId,
            @Valid @RequestBody CreateFacilityBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FacilityRequestResponse response = facilityRequestService.createRequest(facilityId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "List facility booking requests", description = "List all booking requests for a facility. Restricted to ADMIN and FACULTY.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Requests retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Students cannot list all facility requests"),
            @ApiResponse(responseCode = "404", description = "Facility not found")
    })
    public ResponseEntity<Page<FacilityRequestResponse>> getRequestsByFacilityId(
            @PathVariable Long facilityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<FacilityRequestResponse> responses = facilityRequestService.getRequestsByFacilityId(facilityId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get a single facility request", description = "Retrieve a specific booking request. Students may only view their own requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request details retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another user's request"),
            @ApiResponse(responseCode = "404", description = "Request or Facility not found")
    })
    public ResponseEntity<FacilityRequestResponse> getRequestById(
            @PathVariable Long facilityId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FacilityRequestResponse response = facilityRequestService.getRequestById(facilityId, requestId, principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Update booking request", description = "Modify an existing booking request. Students can only modify their own PENDING requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update payload or state"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot modify another user's request"),
            @ApiResponse(responseCode = "404", description = "Request or Facility not found"),
            @ApiResponse(responseCode = "409", description = "Schedule conflict")
    })
    public ResponseEntity<FacilityRequestResponse> updateRequest(
            @PathVariable Long facilityId,
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateFacilityBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FacilityRequestResponse response = facilityRequestService.updateRequest(facilityId, requestId, request, principal);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{requestId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Update request status (Approve/Reject/Cancel)", description = "Approve, reject, or cancel a facility booking request. Approver ID is recorded from JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or FACULTY role"),
            @ApiResponse(responseCode = "404", description = "Request or Facility not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with an already approved booking")
    })
    public ResponseEntity<FacilityRequestResponse> updateRequestStatus(
            @PathVariable Long facilityId,
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateFacilityRequestStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        FacilityRequestResponse response = facilityRequestService.updateRequestStatus(facilityId, requestId, request, principal);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Cancel or delete request", description = "Cancel a booking request. Students can only cancel their own request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Request cancelled successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot cancel another user's request"),
            @ApiResponse(responseCode = "404", description = "Request or Facility not found")
    })
    public ResponseEntity<Void> cancelOrDeleteRequest(
            @PathVariable Long facilityId,
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {

        facilityRequestService.cancelOrDeleteRequest(facilityId, requestId, principal);
        return ResponseEntity.noContent().build();
    }
}
