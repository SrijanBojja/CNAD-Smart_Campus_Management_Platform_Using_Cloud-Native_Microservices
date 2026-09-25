package com.smartcampus.auth.controller;

import com.smartcampus.auth.dto.response.UserValidationResponse;
import com.smartcampus.auth.exception.BadRequestException;
import com.smartcampus.auth.exception.GlobalExceptionHandler;
import com.smartcampus.auth.exception.ResourceNotFoundException;
import com.smartcampus.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private InternalUserController internalUserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(internalUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/internal/users/{userId}/validation returns 200 with active=true and hasRequiredRole=true")
    void testValidateUserSuccess_HasRole() throws Exception {
        UserValidationResponse response = new UserValidationResponse(101L, true, true);
        when(authService.validateUser(101L, "STUDENT")).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/users/101/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasRequiredRole").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/internal/users/{userId}/validation returns 200 with hasRequiredRole=false for role mismatch")
    void testValidateUserSuccess_RoleMismatch() throws Exception {
        UserValidationResponse response = new UserValidationResponse(102L, true, false);
        when(authService.validateUser(102L, "STUDENT")).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/users/102/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(102L))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasRequiredRole").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/internal/users/{userId}/validation returns 404 when user is not found")
    void testValidateUserNotFound() throws Exception {
        when(authService.validateUser(999L, "STUDENT"))
                .thenThrow(new ResourceNotFoundException("User not found with ID: 999"));

        mockMvc.perform(get("/api/v1/internal/users/999/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/internal/users/{userId}/validation returns 200 when requiredRole is omitted")
    void testValidateUserOmittedRequiredRole() throws Exception {
        UserValidationResponse response = new UserValidationResponse(101L, true, true);
        when(authService.validateUser(101L, null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/users/101/validation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101L))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasRequiredRole").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/internal/users/{userId}/validation returns 400 when requiredRole is invalid")
    void testValidateUserInvalidRole() throws Exception {
        when(authService.validateUser(101L, "INVALID_ROLE"))
                .thenThrow(new BadRequestException("Invalid requiredRole 'INVALID_ROLE'. Valid roles are: [ADMIN, FACULTY, STUDENT]"));

        mockMvc.perform(get("/api/v1/internal/users/101/validation")
                        .param("requiredRole", "INVALID_ROLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid requiredRole 'INVALID_ROLE'. Valid roles are: [ADMIN, FACULTY, STUDENT]"));
    }
}
