package com.smartcampus.facility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.facility.config.SecurityConfig;
import com.smartcampus.facility.dto.request.CreateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequestStatusRequest;
import com.smartcampus.facility.dto.response.FacilityRequestResponse;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import com.smartcampus.facility.security.JwtAccessDeniedHandler;
import com.smartcampus.facility.security.JwtAuthenticationEntryPoint;
import com.smartcampus.facility.security.JwtTokenProvider;
import com.smartcampus.facility.service.FacilityRequestService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FacilityRequestController.class)
@Import(SecurityConfig.class)
class FacilityRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacilityRequestService facilityRequestService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create booking request as STUDENT returns 201 Created")
    void testCreateRequestAsStudentSuccess() throws Exception {
        CreateFacilityBookingRequest request = new CreateFacilityBookingRequest(
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Robotics Club Workshop"
        );

        FacilityRequestResponse response = new FacilityRequestResponse();
        response.setId(10L);
        response.setFacilityId(1L);
        response.setStatus(FacilityRequestStatus.PENDING);
        response.setPurpose("Robotics Club Workshop");

        when(facilityRequestService.createRequest(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/facilities/1/requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("List all facility requests as STUDENT returns 403 Forbidden")
    void testListRequestsAsStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/1/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("List all facility requests as FACULTY returns 200 OK")
    void testListRequestsAsFacultySuccess() throws Exception {
        FacilityRequestResponse response = new FacilityRequestResponse();
        response.setId(10L);
        response.setFacilityId(1L);
        response.setStatus(FacilityRequestStatus.PENDING);

        when(facilityRequestService.getRequestsByFacilityId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/facilities/1/requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10L));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Update request status as STUDENT returns 403 Forbidden")
    void testUpdateRequestStatusAsStudentForbidden() throws Exception {
        UpdateFacilityRequestStatusRequest request = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Self approve"
        );

        mockMvc.perform(patch("/api/v1/facilities/1/requests/10/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Update request status as ADMIN returns 200 OK")
    void testUpdateRequestStatusAsAdminSuccess() throws Exception {
        UpdateFacilityRequestStatusRequest request = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Approved by Admin"
        );

        FacilityRequestResponse response = new FacilityRequestResponse();
        response.setId(10L);
        response.setStatus(FacilityRequestStatus.APPROVED);

        when(facilityRequestService.updateRequestStatus(eq(1L), eq(10L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/facilities/1/requests/10/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
