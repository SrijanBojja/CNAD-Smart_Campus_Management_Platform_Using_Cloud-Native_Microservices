package com.smartcampus.facility.dto.request;

import com.smartcampus.facility.entity.FacilityStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to update facility status")
public class UpdateFacilityStatusRequest {

    @NotNull(message = "Facility status is required")
    @Schema(description = "New status of the facility", example = "MAINTENANCE")
    private FacilityStatus status;

    public UpdateFacilityStatusRequest() {
    }

    public UpdateFacilityStatusRequest(FacilityStatus status) {
        this.status = status;
    }

    public FacilityStatus getStatus() {
        return status;
    }

    public void setStatus(FacilityStatus status) {
        this.status = status;
    }
}
