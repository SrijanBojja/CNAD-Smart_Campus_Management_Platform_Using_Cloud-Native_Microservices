package com.smartcampus.auth.service;

import com.smartcampus.auth.dto.request.LoginRequest;
import com.smartcampus.auth.dto.response.AuthResponse;
import com.smartcampus.auth.dto.response.UserDto;
import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.exception.InvalidCredentialsException;
import com.smartcampus.auth.exception.ResourceNotFoundException;
import com.smartcampus.auth.exception.UserDisabledException;
import com.smartcampus.auth.repository.UserRepository;
import com.smartcampus.auth.security.JwtTokenProvider;
import com.smartcampus.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    private User sampleUser;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, userRepository, tokenProvider);
        passwordEncoder = new BCryptPasswordEncoder();

        studentRole = new Role(1L, "STUDENT", "Student Role");
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("srijan");
        sampleUser.setEmail("srijan@example.com");
        sampleUser.setPasswordHash(passwordEncoder.encode("Password123"));
        sampleUser.setFirstName("Srijan");
        sampleUser.setLastName("Bojja");
        sampleUser.setIsActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
        sampleUser.setUpdatedAt(LocalDateTime.now());
        sampleUser.setRoles(Set.of(studentRole));
    }

    @Test
    @DisplayName("Password hashing verification with BCrypt")
    void testPasswordHashing() {
        String rawPassword = "Password123";
        String encodedHash = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedHash);
        assertTrue(passwordEncoder.matches(rawPassword, encodedHash));
        assertFalse(passwordEncoder.matches("WrongPassword", encodedHash));
    }

    @Test
    @DisplayName("Successful login returns valid AuthResponse with UserDto and JWT")
    void testSuccessfulLogin() {
        LoginRequest request = new LoginRequest("srijan", "Password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        when(userRepository.findByUsernameOrEmail("srijan", "srijan"))
                .thenReturn(Optional.of(sampleUser));
        when(tokenProvider.generateToken(eq(1L), eq("srijan"), eq("srijan@example.com"), anyList()))
                .thenReturn("mocked.jwt.token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("srijan", response.getUser().getUsername());
        assertEquals("srijan@example.com", response.getUser().getEmail());
        assertEquals(List.of("STUDENT"), response.getUser().getRoles());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Invalid credentials should throw InvalidCredentialsException")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("srijan", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByUsernameOrEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Inactive user should throw UserDisabledException upon login")
    void testLoginDisabledUser() {
        sampleUser.setIsActive(false);
        LoginRequest request = new LoginRequest("srijan", "Password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        when(userRepository.findByUsernameOrEmail("srijan", "srijan"))
                .thenReturn(Optional.of(sampleUser));

        assertThrows(UserDisabledException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("getCurrentUser returns UserDto for valid username")
    void testGetCurrentUserSuccess() {
        when(userRepository.findByUsername("srijan")).thenReturn(Optional.of(sampleUser));

        UserDto userDto = authService.getCurrentUser("srijan");

        assertNotNull(userDto);
        assertEquals(1L, userDto.getId());
        assertEquals("srijan", userDto.getUsername());
        assertEquals("srijan@example.com", userDto.getEmail());
        assertEquals(List.of("STUDENT"), userDto.getRoles());
    }

    @Test
    @DisplayName("getCurrentUser throws ResourceNotFoundException for unknown username")
    void testGetCurrentUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser("unknown"));
    }
}
