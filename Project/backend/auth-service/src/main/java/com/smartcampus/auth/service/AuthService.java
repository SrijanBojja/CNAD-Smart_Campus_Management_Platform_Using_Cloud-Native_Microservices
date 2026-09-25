package com.smartcampus.auth.service;

import com.smartcampus.auth.dto.request.LoginRequest;
import com.smartcampus.auth.dto.response.AuthResponse;
import com.smartcampus.auth.dto.response.UserDto;

import com.smartcampus.auth.dto.response.UserValidationResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserDto getCurrentUser(String username);
    UserValidationResponse validateUser(Long userId, String requiredRole);
}
