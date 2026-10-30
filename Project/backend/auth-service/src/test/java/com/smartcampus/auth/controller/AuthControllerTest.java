package com.smartcampus.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcampus.auth.dto.request.LoginRequest;
import com.smartcampus.auth.dto.request.RegisterStudentRequest;
import com.smartcampus.auth.dto.response.AuthResponse;
import com.smartcampus.auth.dto.response.UserDto;
import com.smartcampus.auth.exception.DuplicateResourceException;
import com.smartcampus.auth.exception.GlobalExceptionHandler;
import com.smartcampus.auth.exception.InvalidCredentialsException;
import com.smartcampus.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 and AuthResponse on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("srijan", "Password123");
        UserDto userDto = new UserDto(1L, "srijan", "srijan@example.com", List.of("STUDENT"));
        AuthResponse authResponse = new AuthResponse("mocked.jwt.token", "Bearer", 86400000L, userDto);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000L))
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.user.username").value("srijan"))
                .andExpect(jsonPath("$.user.email").value("srijan@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 400 Bad Request on validation failure (empty credentials)")
    void testLoginValidationFailure() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 401 Unauthorized on invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("srijan", "WrongPass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me returns 200 and UserDto for authenticated user")
    void testGetMeSuccess() throws Exception {
        UserDto userDto = new UserDto(1L, "srijan", "srijan@example.com", List.of("STUDENT"));
        when(authService.getCurrentUser("srijan")).thenReturn(userDto);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "srijan", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("srijan"))
                .andExpect(jsonPath("$.email").value("srijan@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me returns 401 Unauthorized when unauthenticated")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/students returns 201 Created and UserDto on valid request")
    void testRegisterStudentSuccess() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest("john_doe", "john.doe@campus.edu", "SecretPass123", "John", "Doe");
        UserDto userDto = new UserDto(5L, "john_doe", "john.doe@campus.edu", "John", "Doe", List.of("STUDENT"));

        when(authService.registerStudent(any(RegisterStudentRequest.class))).thenReturn(userDto);

        mockMvc.perform(post("/api/v1/auth/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john.doe@campus.edu"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/students returns 400 Bad Request on missing required fields")
    void testRegisterStudentValidationFailure() throws Exception {
        RegisterStudentRequest invalidRequest = new RegisterStudentRequest("", "not-an-email", "");

        mockMvc.perform(post("/api/v1/auth/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/students returns 409 Conflict when username/email already exists")
    void testRegisterStudentDuplicateConflict() throws Exception {
        RegisterStudentRequest request = new RegisterStudentRequest("john_doe", "john.doe@campus.edu", "SecretPass123");

        when(authService.registerStudent(any(RegisterStudentRequest.class)))
                .thenThrow(new DuplicateResourceException("Username 'john_doe' is already taken"));

        mockMvc.perform(post("/api/v1/auth/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username 'john_doe' is already taken"));
    }
}
