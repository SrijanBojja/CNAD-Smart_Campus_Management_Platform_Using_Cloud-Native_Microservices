package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.MaintenancePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to update maintenance record details")
public class UpdateMaintenanceRecordRequest {

    @NotBlank(message = "Issue description is required")
    @Size(max = 2000, message = "Issue description must not exceed 2000 characters")
    @Schema(description = "Detailed description of the maintenance issue", example = "Projector lamp replaced, testing HDMI output")
    private String issueDescription;

    @NotNull(message = "Priority is required")
    @Schema(description = "Maintenance priority level", example = "HIGH")
    private MaintenancePriority priority;

    @Schema(description = "Assigned technician user ID", example = "15")
    private Long assignedToUserId;

    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    @Schema(description = "Notes regarding diagnosis or repair", example = "Checked power input, replaced bulb unit")
    private String resolutionNotes;

    public UpdateMaintenanceRecordRequest() {
    }

    public UpdateMaintenanceRecordRequest(String issueDescription, MaintenancePriority priority, Long assignedToUserId, String resolutionNotes) {
        this.issueDescription = issueDescription;
        this.priority = priority;
        this.assignedToUserId = assignedToUserId;
        this.resolutionNotes = resolutionNotes;
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

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
