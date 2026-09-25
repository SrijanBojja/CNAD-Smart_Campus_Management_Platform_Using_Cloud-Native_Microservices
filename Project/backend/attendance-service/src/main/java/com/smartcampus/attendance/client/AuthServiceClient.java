package com.smartcampus.attendance.client;

import com.smartcampus.attendance.client.dto.UserValidationResponseDto;

public interface AuthServiceClient {
    UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken);
}
