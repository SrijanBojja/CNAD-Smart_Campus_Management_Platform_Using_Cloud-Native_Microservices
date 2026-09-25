package com.smartcampus.facility.client.dto;

public class UserValidationResponseDto {

    private Long userId;
    private boolean active;
    private boolean hasRequiredRole;

    public UserValidationResponseDto() {
    }

    public UserValidationResponseDto(Long userId, boolean active, boolean hasRequiredRole) {
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
