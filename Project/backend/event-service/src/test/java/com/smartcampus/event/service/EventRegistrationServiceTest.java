package com.smartcampus.event.service;

import com.smartcampus.event.dto.response.EventRegistrationResponse;
import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventRegistration;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.exception.BadRequestException;
import com.smartcampus.event.exception.DuplicateResourceException;
import com.smartcampus.event.exception.ResourceNotFoundException;
import com.smartcampus.event.repository.EventRegistrationRepository;
import com.smartcampus.event.repository.EventRepository;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.impl.EventRegistrationServiceImpl;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @InjectMocks
    private EventRegistrationServiceImpl registrationService;

    private Event event;
    private EventRegistration registration;
    private UserPrincipal studentPrincipal;
    private UserPrincipal otherStudentPrincipal;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        studentPrincipal = new UserPrincipal(101L, "alice", "alice@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        otherStudentPrincipal = new UserPrincipal(102L, "bob", "bob@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        adminPrincipal = new UserPrincipal(999L, "admin", "admin@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        event = new Event(
                "Hackathon 2026",
                "Coding challenge",
                EventType.TECHNICAL,
                LocalDateTime.of(2026, 10, 1, 9, 0),
                LocalDateTime.of(2026, 10, 1, 17, 0),
                "Hall A",
                201L,
                2,
                EventStatus.UPCOMING
        );
        event.setId(1L);

        registration = new EventRegistration(event, 101L, RegistrationStatus.REGISTERED);
        registration.setId(10L);
    }

    @Test
    @DisplayName("Register for event successfully")
    void testRegisterSuccess() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndUserId(1L, 101L)).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.REGISTERED)).thenReturn(0L);
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(registration);

        EventRegistrationResponse response = registrationService.registerForEvent(1L, studentPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getEventId());
        assertEquals(101L, response.getUserId());
        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());
        verify(registrationRepository, times(1)).save(any(EventRegistration.class));
    }

    @Test
    @DisplayName("Register throws DuplicateResourceException on duplicate active registration")
    void testRegisterDuplicate() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndUserId(1L, 101L)).thenReturn(Optional.of(registration));

        assertThrows(DuplicateResourceException.class, () ->
                registrationService.registerForEvent(1L, studentPrincipal));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register throws BadRequestException when event capacity is reached")
    void testRegisterCapacityReached() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEventIdAndUserId(1L, 101L)).thenReturn(Optional.empty());
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.REGISTERED)).thenReturn(2L);

        assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(1L, studentPrincipal));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register throws BadRequestException when event is cancelled")
    void testRegisterCancelledEvent() {
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(BadRequestException.class, () ->
                registrationService.registerForEvent(1L, studentPrincipal));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get registration by ID as owner STUDENT succeeds")
    void testGetRegistrationAsOwnerStudent() {
        when(registrationRepository.findByIdAndEventId(10L, 1L)).thenReturn(Optional.of(registration));

        EventRegistrationResponse response = registrationService.getRegistrationById(1L, 10L, studentPrincipal);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(101L, response.getUserId());
    }

    @Test
    @DisplayName("Get registration by ID as other STUDENT throws AccessDeniedException")
    void testGetRegistrationAsOtherStudentDenied() {
        when(registrationRepository.findByIdAndEventId(10L, 1L)).thenReturn(Optional.of(registration));

        assertThrows(AccessDeniedException.class, () ->
                registrationService.getRegistrationById(1L, 10L, otherStudentPrincipal));
    }

    @Test
    @DisplayName("Cancel registration as owner succeeds")
    void testCancelRegistrationAsOwner() {
        when(registrationRepository.findByIdAndEventId(10L, 1L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(registration);

        EventRegistrationResponse response = registrationService.cancelRegistration(1L, 10L, studentPrincipal);

        assertNotNull(response);
        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
        verify(registrationRepository, times(1)).save(registration);
    }

    @Test
    @DisplayName("Cancel registration as other STUDENT throws AccessDeniedException")
    void testCancelRegistrationAsOtherStudentDenied() {
        when(registrationRepository.findByIdAndEventId(10L, 1L)).thenReturn(Optional.of(registration));

        assertThrows(AccessDeniedException.class, () ->
                registrationService.cancelRegistration(1L, 10L, otherStudentPrincipal));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("List registrations for event")
    void testListRegistrationsForEvent() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(registrationRepository.findByEventId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registration)));

        Page<EventRegistrationResponse> page = registrationService.listRegistrationsForEvent(
                1L, null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }
}
