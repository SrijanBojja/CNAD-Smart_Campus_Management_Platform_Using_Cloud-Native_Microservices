package com.smartcampus.notification.client;

import com.smartcampus.notification.client.dto.UserValidationResponseDto;

public interface AuthServiceClient {
    UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken);
}
