package com.smartcampus.student.client;

import com.smartcampus.student.client.dto.UserValidationResponse;

public interface AuthServiceClient {
    UserValidationResponse validateUser(Long userId, String requiredRole, String bearerToken);
}
