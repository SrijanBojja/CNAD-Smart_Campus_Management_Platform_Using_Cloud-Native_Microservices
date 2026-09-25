package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.MaintenancePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to report facility maintenance issue")
public class CreateMaintenanceRecordRequest {

    @NotBlank(message = "Issue description is required")
    @Size(max = 2000, message = "Issue description must not exceed 2000 characters")
    @Schema(description = "Detailed description of the maintenance issue", example = "Projector lamp burned out in room 101")
    private String issueDescription;

    @NotNull(message = "Priority is required")
    @Schema(description = "Maintenance priority level", example = "HIGH")
    private MaintenancePriority priority;

    @Schema(description = "Assigned technician user ID", example = "15")
    private Long assignedToUserId;

    public CreateMaintenanceRecordRequest() {
    }

    public CreateMaintenanceRecordRequest(String issueDescription, MaintenancePriority priority, Long assignedToUserId) {
        this.issueDescription = issueDescription;
        this.priority = priority;
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

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }
}
