package com.smartcampus.facility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.facility.config.SecurityConfig;
import com.smartcampus.facility.dto.request.CreateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityStatusRequest;
import com.smartcampus.facility.dto.response.FacilityResponse;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.security.JwtAccessDeniedHandler;
import com.smartcampus.facility.security.JwtAuthenticationEntryPoint;
import com.smartcampus.facility.security.JwtTokenProvider;
import com.smartcampus.facility.service.FacilityService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacilityController.class)
@Import(SecurityConfig.class)
class FacilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacilityService facilityService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get facility by ID as STUDENT returns 200 OK")
    void testGetFacilityAsStudent() throws Exception {
        FacilityResponse response = new FacilityResponse();
        response.setId(1L);
        response.setName("Lecture Hall 101");
        response.setFacilityType(FacilityType.CLASSROOM);
        response.setStatus(FacilityStatus.AVAILABLE);

        when(facilityService.getFacilityById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/facilities/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Lecture Hall 101"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create facility as STUDENT returns 403 Forbidden")
    void testCreateFacilityAsStudentForbidden() throws Exception {
        CreateFacilityRequest request = new CreateFacilityRequest(
                "Seminar Hall", FacilityType.SEMINAR_HALL, "Hall desc", "Main Bldg", "SH-1", 100, FacilityStatus.AVAILABLE
        );

        mockMvc.perform(post("/api/v1/facilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Create facility as ADMIN returns 201 Created")
    void testCreateFacilityAsAdminSuccess() throws Exception {
        CreateFacilityRequest request = new CreateFacilityRequest(
                "Seminar Hall", FacilityType.SEMINAR_HALL, "Hall desc", "Main Bldg", "SH-1", 100, FacilityStatus.AVAILABLE
        );

        FacilityResponse response = new FacilityResponse();
        response.setId(1L);
        response.setName("Seminar Hall");

        when(facilityService.createFacility(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/facilities")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Seminar Hall"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update facility status as ADMIN returns 200 OK")
    void testUpdateFacilityStatusAsAdminSuccess() throws Exception {
        UpdateFacilityStatusRequest request = new UpdateFacilityStatusRequest(FacilityStatus.MAINTENANCE);

        FacilityResponse response = new FacilityResponse();
        response.setId(1L);
        response.setStatus(FacilityStatus.MAINTENANCE);

        when(facilityService.updateFacilityStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/facilities/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Delete facility as ADMIN returns 204 No Content")
    void testDeleteFacilityAsAdminSuccess() throws Exception {
        doNothing().when(facilityService).deleteFacility(1L);

        mockMvc.perform(delete("/api/v1/facilities/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
