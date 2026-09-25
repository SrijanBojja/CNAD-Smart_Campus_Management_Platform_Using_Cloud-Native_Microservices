package com.smartcampus.academic.client;

import com.smartcampus.academic.client.dto.UserValidationResponseDto;

public interface AuthServiceClient {

    UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken);
}
