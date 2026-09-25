package com.smartcampus.event.client;

import com.smartcampus.event.client.dto.UserValidationResponseDto;

public interface AuthServiceClient {

    UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken);
}
