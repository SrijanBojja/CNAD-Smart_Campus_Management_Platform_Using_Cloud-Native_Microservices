package com.smartcampus.facility.dto.response;

import com.smartcampus.facility.entity.MaintenancePriority;
import com.smartcampus.facility.entity.MaintenanceRecord;
import com.smartcampus.facility.entity.MaintenanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response representing maintenance record details")
public class MaintenanceRecordResponse {

    @Schema(description = "Maintenance record ID", example = "1")
    private Long id;

    @Schema(description = "Facility ID", example = "5")
    private Long facilityId;

    @Schema(description = "Facility Name", example = "Auditorium Main Hall")
    private String facilityName;

    @Schema(description = "User ID who reported the issue", example = "12")
    private Long reportedByUserId;

    @Schema(description = "User ID assigned to fix the issue", example = "15")
    private Long assignedToUserId;

    @Schema(description = "Issue description", example = "Projector lamp burned out")
    private String issueDescription;

    @Schema(description = "Priority", example = "HIGH")
    private MaintenancePriority priority;

    @Schema(description = "Maintenance status", example = "OPEN")
    private MaintenanceStatus status;

    @Schema(description = "Timestamp when reported")
    private LocalDateTime reportedAt;

    @Schema(description = "Timestamp when resolved")
    private LocalDateTime resolvedAt;

    @Schema(description = "Resolution notes")
    private String resolutionNotes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    public MaintenanceRecordResponse() {
    }

    public static MaintenanceRecordResponse fromEntity(MaintenanceRecord record) {
        if (record == null) {
            return null;
        }
        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        response.setId(record.getId());
        if (record.getFacility() != null) {
            response.setFacilityId(record.getFacility().getId());
            response.setFacilityName(record.getFacility().getName());
        }
        response.setReportedByUserId(record.getReportedByUserId());
        response.setAssignedToUserId(record.getAssignedToUserId());
        response.setIssueDescription(record.getIssueDescription());
        response.setPriority(record.getPriority());
        response.setStatus(record.getStatus());
        response.setReportedAt(record.getReportedAt());
        response.setResolvedAt(record.getResolvedAt());
        response.setResolutionNotes(record.getResolutionNotes());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
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

    public Long getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(Long reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public MaintenancePriority getPriority() {
        return priority;
    }

    public void setPriority(MaintenancePriority priority) {
        this.priority = priority;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
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
