package com.smartcampus.auth.controller;

import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.repository.RoleRepository;
import com.smartcampus.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InternalUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User studentUser;
    private User facultyUser;

    @BeforeEach
    void setUp() {
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseGet(() -> roleRepository.save(new Role("STUDENT", "Student role")));
        Role facultyRole = roleRepository.findByName("FACULTY")
                .orElseGet(() -> roleRepository.save(new Role("FACULTY", "Faculty role")));

        studentUser = new User();
        studentUser.setUsername("integration_student");
        studentUser.setEmail("integration_student@smartcampus.edu");
        studentUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        studentUser.setFirstName("Alice");
        studentUser.setLastName("Student");
        studentUser.setIsActive(true);
        studentUser.setCreatedAt(LocalDateTime.now());
        studentUser.setUpdatedAt(LocalDateTime.now());
        studentUser.setRoles(Set.of(studentRole));
        studentUser = userRepository.save(studentUser);

        facultyUser = new User();
        facultyUser.setUsername("integration_faculty");
        facultyUser.setEmail("integration_faculty@smartcampus.edu");
        facultyUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        facultyUser.setFirstName("Bob");
        facultyUser.setLastName("Faculty");
        facultyUser.setIsActive(true);
        facultyUser.setCreatedAt(LocalDateTime.now());
        facultyUser.setUpdatedAt(LocalDateTime.now());
        facultyUser.setRoles(Set.of(facultyRole));
        facultyUser = userRepository.save(facultyUser);
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN caller validates existing STUDENT user with requiredRole=STUDENT -> 200 OK")
    void testAdminValidatesStudentSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(studentUser.getId()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasRequiredRole").value(true));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN caller validates user with non-matching role -> 200 OK with hasRequiredRole=false")
    void testAdminValidatesRoleMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + facultyUser.getId() + "/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(facultyUser.getId()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hasRequiredRole").value(false));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN caller validates non-existent user -> 404 NOT_FOUND")
    void testAdminValidatesNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/999999/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN caller validates with missing requiredRole -> 400 BAD_REQUEST")
    void testAdminValidatesMissingRole() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN caller validates with invalid requiredRole -> 400 BAD_REQUEST")
    void testAdminValidatesInvalidRole() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .param("requiredRole", "UNKNOWN_ROLE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Unauthenticated caller -> 401 UNAUTHORIZED")
    void testUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    @DisplayName("Authenticated non-ADMIN caller (STUDENT) -> 403 FORBIDDEN")
    void testNonAdminStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "faculty_user", roles = {"FACULTY"})
    @DisplayName("Authenticated non-ADMIN caller (FACULTY) -> 403 FORBIDDEN")
    void testNonAdminFacultyForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/" + studentUser.getId() + "/validation")
                        .param("requiredRole", "STUDENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
