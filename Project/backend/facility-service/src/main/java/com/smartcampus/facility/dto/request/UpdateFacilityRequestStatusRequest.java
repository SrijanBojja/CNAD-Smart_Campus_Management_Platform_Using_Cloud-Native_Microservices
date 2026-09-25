package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.FacilityRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to change facility request status")
public class UpdateFacilityRequestStatusRequest {

    @NotNull(message = "Request status is required")
    @Schema(description = "New status of the booking request", example = "APPROVED")
    private FacilityRequestStatus status;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Schema(description = "Approval/Rejection/Cancellation remarks", example = "Booking approved by Department Head")
    private String remarks;

    public UpdateFacilityRequestStatusRequest() {
    }

    public UpdateFacilityRequestStatusRequest(FacilityRequestStatus status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }

    public FacilityRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FacilityRequestStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
