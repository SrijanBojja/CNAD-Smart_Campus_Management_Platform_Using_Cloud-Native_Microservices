package com.smartcampus.facility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.facility.config.SecurityConfig;
import com.smartcampus.facility.dto.request.CreateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceStatusRequest;
import com.smartcampus.facility.dto.response.MaintenanceRecordResponse;
import com.smartcampus.facility.entity.MaintenancePriority;
import com.smartcampus.facility.entity.MaintenanceStatus;
import com.smartcampus.facility.security.JwtAccessDeniedHandler;
import com.smartcampus.facility.security.JwtAuthenticationEntryPoint;
import com.smartcampus.facility.security.JwtTokenProvider;
import com.smartcampus.facility.service.MaintenanceRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceController.class)
@Import(SecurityConfig.class)
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceRecordService maintenanceRecordService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Report maintenance as STUDENT returns 403 Forbidden")
    void testCreateMaintenanceAsStudentForbidden() throws Exception {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                "Broken chair", MaintenancePriority.LOW, null
        );

        mockMvc.perform(post("/api/v1/facilities/1/maintenance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Report maintenance as FACULTY returns 201 Created")
    void testCreateMaintenanceAsFacultySuccess() throws Exception {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                "Projector bulb blown", MaintenancePriority.HIGH, 15L
        );

        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        response.setId(20L);
        response.setFacilityId(1L);
        response.setStatus(MaintenanceStatus.OPEN);
        response.setPriority(MaintenancePriority.HIGH);

        when(maintenanceRecordService.createMaintenanceRecord(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/facilities/1/maintenance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update maintenance status as ADMIN returns 200 OK")
    void testUpdateMaintenanceStatusAsAdminSuccess() throws Exception {
        UpdateMaintenanceStatusRequest request = new UpdateMaintenanceStatusRequest(
                MaintenanceStatus.RESOLVED, 15L, "Replaced bulb"
        );

        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        response.setId(20L);
        response.setStatus(MaintenanceStatus.RESOLVED);

        when(maintenanceRecordService.updateMaintenanceStatus(eq(1L), eq(20L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/facilities/1/maintenance/20/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }
}
