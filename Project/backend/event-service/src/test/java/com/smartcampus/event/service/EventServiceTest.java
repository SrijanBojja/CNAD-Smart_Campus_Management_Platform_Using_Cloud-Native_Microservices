package com.smartcampus.event.service;

import com.smartcampus.event.client.AuthServiceClient;
import com.smartcampus.event.client.dto.UserValidationResponseDto;
import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.request.UpdateEventRequest;
import com.smartcampus.event.dto.response.EventResponse;
import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.exception.BadRequestException;
import com.smartcampus.event.exception.ResourceNotFoundException;
import com.smartcampus.event.repository.EventRegistrationRepository;
import com.smartcampus.event.repository.EventRepository;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event event;
    private CreateEventRequest createRequest;
    private UpdateEventRequest updateRequest;
    private UserPrincipal facultyPrincipal;
    private UserPrincipal adminPrincipal;
    private UserPrincipal otherFacultyPrincipal;

    @BeforeEach
    void setUp() {
        facultyPrincipal = new UserPrincipal(201L, "prof_smith", "prof_smith@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));

        adminPrincipal = new UserPrincipal(999L, "admin", "admin@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        otherFacultyPrincipal = new UserPrincipal(202L, "prof_jones", "prof_jones@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));

        event = new Event(
                "Hackathon 2026",
                "Coding challenge",
                EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A",
                201L,
                100,
                EventStatus.UPCOMING
        );
        event.setId(1L);

        createRequest = new CreateEventRequest(
                "Hackathon 2026",
                "Coding challenge",
                EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A",
                201L,
                100
        );

        updateRequest = new UpdateEventRequest(
                "Hackathon 2026 Updated",
                "Updated challenge",
                EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 30),
                LocalDateTime.of(2026, 10, 1, 17, 30),
                "Hall B",
                120,
                EventStatus.UPCOMING
        );
    }

    @Test
    @DisplayName("Create event successfully as FACULTY")
    void testCreateEventSuccess() {
        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, true, true));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventResponse response = eventService.createEvent(createRequest, facultyPrincipal, "Bearer token");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Hackathon 2026", response.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Create event throws BadRequestException when start time is after end time")
    void testCreateEventInvalidDateRange() {
        createRequest.setStartDateTime(LocalDateTime.of(2026, 10, 1, 18, 0));
        createRequest.setEndDateTime(LocalDateTime.of(2026, 10, 1, 9, 0));

        assertThrows(BadRequestException.class, () ->
                eventService.createEvent(createRequest, facultyPrincipal, "Bearer token"));
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("Create event throws BadRequestException when organizer validation fails")
    void testCreateEventInvalidOrganizer() {
        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, false, false));

        assertThrows(BadRequestException.class, () ->
                eventService.createEvent(createRequest, facultyPrincipal, "Bearer token"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get event by ID success")
    void testGetEventByIdSuccess() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventResponse response = eventService.getEventById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Hackathon 2026", response.getTitle());
    }

    @Test
    @DisplayName("Get event by ID throws ResourceNotFoundException when event missing")
    void testGetEventByIdNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                eventService.getEventById(99L));
    }

    @Test
    @DisplayName("List events paginated")
    void testListEvents() {
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<EventResponse> result = eventService.listEvents(
                EventType.TECHNICAL, EventStatus.UPCOMING, 201L, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Update event as owner FACULTY succeeds")
    void testUpdateEventAsOwnerFacultySuccess() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventResponse response = eventService.updateEvent(1L, updateRequest, facultyPrincipal, "Bearer token");

        assertNotNull(response);
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    @DisplayName("Update event as other FACULTY throws AccessDeniedException")
    void testUpdateEventAsOtherFacultyDenied() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class, () ->
                eventService.updateEvent(1L, updateRequest, otherFacultyPrincipal, "Bearer token"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delete event with no registrations deletes entity")
    void testDeleteEventNoRegistrations() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(1L, com.smartcampus.event.entity.RegistrationStatus.REGISTERED))
                .thenReturn(0L);

        eventService.deleteOrCancelEvent(1L, facultyPrincipal);

        verify(eventRepository, times(1)).delete(event);
    }

    @Test
    @DisplayName("Delete event with active registrations cancels event")
    void testDeleteEventWithRegistrationsCancels() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(1L, com.smartcampus.event.entity.RegistrationStatus.REGISTERED))
                .thenReturn(5L);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        eventService.deleteOrCancelEvent(1L, facultyPrincipal);

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        verify(eventRepository, times(1)).save(event);
        verify(eventRepository, never()).delete(event);
    }
}
