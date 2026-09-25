package com.smartcampus.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload for internal user validation")
public class UserValidationResponse {

    @Schema(description = "Unique numeric identifier of the user", example = "101")
    @JsonProperty("userId")
    private Long userId;

    @Schema(description = "Whether the user account is active", example = "true")
    @JsonProperty("active")
    private boolean active;

    @Schema(description = "Whether the user possesses the required role", example = "true")
    @JsonProperty("hasRequiredRole")
    private boolean hasRequiredRole;

    public UserValidationResponse() {
    }

    public UserValidationResponse(Long userId, boolean active, boolean hasRequiredRole) {
        this.userId = userId;
        this.active = active;
        this.hasRequiredRole = hasRequiredRole;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHasRequiredRole() {
        return hasRequiredRole;
    }

    public void setHasRequiredRole(boolean hasRequiredRole) {
        this.hasRequiredRole = hasRequiredRole;
    }
}
