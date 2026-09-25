package com.smartcampus.facility.dto.response;

import com.smartcampus.facility.entity.FacilityRequest;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Response representing facility request/booking details")
public class FacilityRequestResponse {

    @Schema(description = "Request ID", example = "1")
    private Long id;

    @Schema(description = "Facility ID", example = "5")
    private Long facilityId;

    @Schema(description = "Facility Name", example = "Auditorium Main Hall")
    private String facilityName;

    @Schema(description = "User ID of requester", example = "10")
    private Long requestedByUserId;

    @Schema(description = "Booking Date", example = "2026-10-15")
    private LocalDate requestDate;

    @Schema(description = "Booking start time", example = "09:00:00")
    private LocalTime startTime;

    @Schema(description = "Booking end time", example = "11:00:00")
    private LocalTime endTime;

    @Schema(description = "Purpose", example = "Annual Computer Science Symposium Keynote")
    private String purpose;

    @Schema(description = "Request status", example = "PENDING")
    private FacilityRequestStatus status;

    @Schema(description = "User ID of approver", example = "3")
    private Long approvedByUserId;

    @Schema(description = "Remarks", example = "Approved for academic event")
    private String remarks;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public FacilityRequestResponse() {
    }

    public static FacilityRequestResponse fromEntity(FacilityRequest request) {
        if (request == null) {
            return null;
        }
        FacilityRequestResponse response = new FacilityRequestResponse();
        response.setId(request.getId());
        if (request.getFacility() != null) {
            response.setFacilityId(request.getFacility().getId());
            response.setFacilityName(request.getFacility().getName());
        }
        response.setRequestedByUserId(request.getRequestedByUserId());
        response.setRequestDate(request.getRequestDate());
        response.setStartTime(request.getStartTime());
        response.setEndTime(request.getEndTime());
        response.setPurpose(request.getPurpose());
        response.setStatus(request.getStatus());
        response.setApprovedByUserId(request.getApprovedByUserId());
        response.setRemarks(request.getRemarks());
        response.setCreatedAt(request.getCreatedAt());
        response.setUpdatedAt(request.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public Long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(Long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public FacilityRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FacilityRequestStatus status) {
        this.status = status;
    }

    public Long getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Long approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
