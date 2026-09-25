package com.smartcampus.event.controller;

import com.smartcampus.event.config.SecurityConfig;
import com.smartcampus.event.dto.response.EventRegistrationResponse;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.security.JwtAccessDeniedHandler;
import com.smartcampus.event.security.JwtAuthenticationEntryPoint;
import com.smartcampus.event.security.JwtTokenProvider;
import com.smartcampus.event.service.EventRegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRegistrationController.class)
@Import(SecurityConfig.class)
class EventRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventRegistrationService registrationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("List event registrations as STUDENT returns 403 Forbidden")
    void testListRegistrationsAsStudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/events/1/registrations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Register for event as STUDENT returns 201 Created")
    void testRegisterForEventAsStudentSuccess() throws Exception {
        EventRegistrationResponse response = new EventRegistrationResponse();
        response.setId(10L);
        response.setEventId(1L);
        response.setUserId(101L);
        response.setStatus(RegistrationStatus.REGISTERED);

        when(registrationService.registerForEvent(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/1/registrations")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.userId").value(101L));
    }
}
