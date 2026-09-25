package com.smartcampus.facility.client;

import com.smartcampus.facility.client.dto.UserValidationResponseDto;

public interface AuthServiceClient {
    UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken);
}
