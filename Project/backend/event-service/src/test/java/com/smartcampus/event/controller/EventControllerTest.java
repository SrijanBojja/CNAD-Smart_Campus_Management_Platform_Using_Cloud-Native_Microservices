package com.smartcampus.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.event.config.SecurityConfig;
import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.response.EventResponse;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.security.JwtAccessDeniedHandler;
import com.smartcampus.event.security.JwtAuthenticationEntryPoint;
import com.smartcampus.event.security.JwtTokenProvider;
import com.smartcampus.event.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @MockBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Get event by ID as STUDENT returns 200 OK")
    void testGetEventAsStudent() throws Exception {
        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setTitle("Hackathon 2026");
        response.setEventType(EventType.TECHNICAL);
        response.setStatus(EventStatus.UPCOMING);

        when(eventService.getEventById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Hackathon 2026"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Create event as STUDENT returns 403 Forbidden")
    void testCreateEventAsStudentForbidden() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Hackathon 2026", "Description", EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A", 201L, 100
        );

        mockMvc.perform(post("/api/v1/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FACULTY")
    @DisplayName("Create event as FACULTY returns 201 Created")
    void testCreateEventAsFacultySuccess() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Hackathon 2026", "Description", EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A", 201L, 100
        );

        EventResponse response = new EventResponse();
        response.setId(1L);
        response.setTitle("Hackathon 2026");

        when(eventService.createEvent(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
