package com.smartcampus.auth.config;

import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.repository.RoleRepository;
import com.smartcampus.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1L, "ADMIN", "Administrator with full system access");
    }

    @Test
    @DisplayName("Should successfully create admin user when no admin exists and password is provided")
    void shouldSeedAdminUser_WhenNoneExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@smartcampus.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("CampusAdmin@123")).thenReturn("$2a$10$hashedPassword");

        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                true,
                "admin",
                "CampusAdmin@123",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("admin", savedUser.getUsername());
        assertEquals("admin@smartcampus.com", savedUser.getEmail());
        assertEquals("$2a$10$hashedPassword", savedUser.getPasswordHash());
        assertEquals("Campus", savedUser.getFirstName());
        assertEquals("Admin", savedUser.getLastName());
        assertTrue(savedUser.getIsActive());
        assertTrue(savedUser.getRoles().contains(adminRole));
    }

    @Test
    @DisplayName("Should skip creation when username already exists")
    void shouldSkipSeeding_WhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                true,
                "admin",
                "CampusAdmin@123",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should skip creation when email already exists")
    void shouldSkipSeeding_WhenEmailAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@smartcampus.com")).thenReturn(true);

        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                true,
                "admin",
                "CampusAdmin@123",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should skip creation when password is null or blank")
    void shouldSkipSeeding_WhenPasswordIsBlank() {
        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                true,
                "admin",
                "   ",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip creation when initializer is disabled")
    void shouldSkipSeeding_WhenDisabled() {
        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                false,
                "admin",
                "CampusAdmin@123",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create ADMIN role if role does not exist in database")
    void shouldCreateAdminRole_WhenRoleNotFoundInDb() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@smartcampus.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(adminRole);
        when(passwordEncoder.encode("CampusAdmin@123")).thenReturn("$2a$10$hashedPassword");

        AdminUserInitializer initializer = new AdminUserInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                true,
                "admin",
                "CampusAdmin@123",
                "admin@smartcampus.com",
                "Campus",
                "Admin"
        );

        initializer.run();

        verify(roleRepository, times(1)).save(any(Role.class));
        verify(userRepository, times(1)).save(any(User.class));
    }
}

