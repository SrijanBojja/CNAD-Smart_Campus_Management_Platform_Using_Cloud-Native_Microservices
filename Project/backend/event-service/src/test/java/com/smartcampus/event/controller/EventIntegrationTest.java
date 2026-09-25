package com.smartcampus.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcampus.event.client.AuthServiceClient;
import com.smartcampus.event.client.dto.UserValidationResponseDto;
import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.request.UpdateEventRequest;
import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventRegistration;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.repository.EventRegistrationRepository;
import com.smartcampus.event.repository.EventRepository;
import com.smartcampus.event.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthServiceClient authServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private Event event1;
    private EventRegistration registration1;

    private String createBearerToken(Long userId, String username, String role) {
        return "Bearer " + jwtTokenProvider.generateToken(userId, username, username + "@smartcampus.edu", List.of(role));
    }

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        eventRepository.deleteAll();

        event1 = new Event(
                "Hackathon 2026",
                "Coding challenge",
                EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A",
                201L,
                1, // capacity of 1 for testing limits
                EventStatus.UPCOMING
        );
        event1 = eventRepository.save(event1);

        registration1 = new EventRegistration(event1, 101L, RegistrationStatus.REGISTERED);
        registration1 = registrationRepository.save(registration1);

        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, true, true));
        when(authServiceClient.validateUser(eq(202L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(202L, true, true));
        when(authServiceClient.validateUser(eq(999L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(999L, true, true));
    }

    @Test
    @DisplayName("1. Unauthenticated request returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + event1.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("2. Student can retrieve event details -> 200 OK")
    void testGetEventAsStudent() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + event1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event1.getId()))
                .andExpect(jsonPath("$.title").value("Hackathon 2026"));
    }

    @Test
    @DisplayName("3. Create event as FACULTY -> 201 Created")
    void testCreateEventAsFaculty() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "AI Workshop", "Deep learning hands-on", EventType.WORKSHOP,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 5, 16, 0),
                "Lab 3", 201L, 50
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("AI Workshop"))
                .andExpect(jsonPath("$.organizerUserId").value(201L));
    }

    @Test
    @DisplayName("4. Create event as ADMIN -> 201 Created")
    void testCreateEventAsAdmin() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Campus Orientation", "Welcome ceremony", EventType.CULTURAL,
                LocalDateTime.of(2026, 10, 10, 9, 0),
                LocalDateTime.of(2026, 10, 10, 12, 0),
                "Auditorium", 201L, 500
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", createBearerToken(999L, "admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Campus Orientation"));
    }

    @Test
    @DisplayName("5. Create event as STUDENT -> 403 Forbidden")
    void testCreateEventAsStudentForbidden() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Student Meetup", "Informal meetup", EventType.OTHER,
                LocalDateTime.of(2026, 10, 15, 18, 0),
                LocalDateTime.of(2026, 10, 15, 20, 0),
                "Cafeteria", 101L, 20
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("6. Create event with invalid date range -> 400 Bad Request")
    void testCreateEventInvalidDateRange() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Invalid Event", "Description", EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 17, 0),
                LocalDateTime.of(2026, 10, 1, 9, 0),
                "Hall A", 201L, 50
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("7. Update event as organizer FACULTY -> 200 OK")
    void testUpdateEventAsOrganizer() throws Exception {
        UpdateEventRequest request = new UpdateEventRequest(
                "Hackathon 2026 - Extended", "Extended coding challenge", EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 18, 0),
                "Hall A & B", 150, EventStatus.UPCOMING
        );

        mockMvc.perform(put("/api/v1/events/" + event1.getId())
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hackathon 2026 - Extended"));
    }

    @Test
    @DisplayName("8. Update event as different FACULTY -> 403 Forbidden")
    void testUpdateEventAsDifferentFacultyForbidden() throws Exception {
        UpdateEventRequest request = new UpdateEventRequest(
                "Hackathon 2026 - Hijacked", "Unauthorized update", EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 18, 0),
                "Hall A", 150, EventStatus.UPCOMING
        );

        mockMvc.perform(put("/api/v1/events/" + event1.getId())
                        .header("Authorization", createBearerToken(202L, "prof_jones", "FACULTY"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("9. Register for event as new STUDENT -> 201 Created (with capacity available)")
    void testRegisterForEvent() throws Exception {
        Event event2 = new Event(
                "Seminar 2026", "AI Seminar", EventType.SEMINAR,
                LocalDateTime.of(2026, 10, 20, 10, 0),
                LocalDateTime.of(2026, 10, 20, 12, 0),
                "Auditorium", 201L, 100, EventStatus.UPCOMING
        );
        event2 = eventRepository.save(event2);

        mockMvc.perform(post("/api/v1/events/" + event2.getId() + "/registrations")
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(102L))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    @Test
    @DisplayName("10. Duplicate registration for same event -> 409 Conflict")
    void testDuplicateRegistration() throws Exception {
        // Alice (userId 101L) is already registered for event1 in setUp()
        mockMvc.perform(post("/api/v1/events/" + event1.getId() + "/registrations")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("11. Registration exceeds event capacity -> 400 Bad Request")
    void testCapacityExceededRegistration() throws Exception {
        // event1 has capacity 1, and Alice (101L) is already registered.
        // Bob (102L) attempts to register.
        mockMvc.perform(post("/api/v1/events/" + event1.getId() + "/registrations")
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("12. Register for CANCELLED event -> 400 Bad Request")
    void testRegisterCancelledEvent() throws Exception {
        Event cancelledEvent = new Event(
                "Cancelled Workshop", "Description", EventType.WORKSHOP,
                LocalDateTime.of(2026, 10, 25, 10, 0),
                LocalDateTime.of(2026, 10, 25, 12, 0),
                "Hall C", 201L, 50, EventStatus.CANCELLED
        );
        cancelledEvent = eventRepository.save(cancelledEvent);

        mockMvc.perform(post("/api/v1/events/" + cancelledEvent.getId() + "/registrations")
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("13. Student accesses own registration -> 200 OK")
    void testStudentAccessOwnRegistration() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + event1.getId() + "/registrations/" + registration1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registration1.getId()))
                .andExpect(jsonPath("$.userId").value(101L));
    }

    @Test
    @DisplayName("14. Student accesses another student's registration -> 403 Forbidden")
    void testStudentAccessOtherRegistrationForbidden() throws Exception {
        // Bob (userId 102L) tries to view Alice's registration (registration1, userId 101L)
        mockMvc.perform(get("/api/v1/events/" + event1.getId() + "/registrations/" + registration1.getId())
                        .header("Authorization", createBearerToken(102L, "bob", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("15. Student cannot list all registrations -> 403 Forbidden")
    void testStudentListRegistrationsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + event1.getId() + "/registrations")
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("16. Student cancels own registration -> 200 OK")
    void testStudentCancelOwnRegistration() throws Exception {
        mockMvc.perform(delete("/api/v1/events/" + event1.getId() + "/registrations/" + registration1.getId())
                        .header("Authorization", createBearerToken(101L, "alice", "STUDENT"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("17. Delete event with active registrations cancels event -> 204 No Content")
    void testDeleteEventWithRegistrations() throws Exception {
        mockMvc.perform(delete("/api/v1/events/" + event1.getId())
                        .header("Authorization", createBearerToken(201L, "prof_smith", "FACULTY")))
                .andExpect(status().isNoContent());

        Event afterDelete = eventRepository.findById(event1.getId()).orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(afterDelete);
        org.junit.jupiter.api.Assertions.assertEquals(EventStatus.CANCELLED, afterDelete.getStatus());
    }
}
