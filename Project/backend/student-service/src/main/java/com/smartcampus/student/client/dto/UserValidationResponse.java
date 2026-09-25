package com.smartcampus.student.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserValidationResponse {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("active")
    private boolean active;

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
