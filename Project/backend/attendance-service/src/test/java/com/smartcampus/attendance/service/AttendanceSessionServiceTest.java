package com.smartcampus.attendance.service;

import com.smartcampus.attendance.client.AcademicServiceClient;
import com.smartcampus.attendance.client.AuthServiceClient;
import com.smartcampus.attendance.client.dto.SubjectResponseDto;
import com.smartcampus.attendance.client.dto.UserValidationResponseDto;
import com.smartcampus.attendance.dto.request.CreateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceSessionRequest;
import com.smartcampus.attendance.dto.response.AttendanceSessionResponse;
import com.smartcampus.attendance.entity.AttendanceSession;
import com.smartcampus.attendance.exception.BadRequestException;
import com.smartcampus.attendance.exception.ResourceNotFoundException;
import com.smartcampus.attendance.repository.AttendanceSessionRepository;
import com.smartcampus.attendance.service.impl.AttendanceSessionServiceImpl;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceSessionServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private AcademicServiceClient academicServiceClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private AttendanceSessionServiceImpl sessionService;

    private AttendanceSession session;
    private CreateAttendanceSessionRequest createRequest;
    private UpdateAttendanceSessionRequest updateRequest;

    @BeforeEach
    void setUp() {
        session = new AttendanceSession(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );
        session.setId(1L);

        createRequest = new CreateAttendanceSessionRequest(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );

        updateRequest = new UpdateAttendanceSessionRequest(
                201L, LocalDate.of(2026, 9, 26),
                LocalTime.of(10, 0), LocalTime.of(11, 30), "Room-305",
                "2026-2027", 3, "B"
        );
    }

    @Test
    @DisplayName("Create session successfully")
    void testCreateSessionSuccess() {
        when(academicServiceClient.getSubjectById(eq(10L), any()))
                .thenReturn(new SubjectResponseDto(10L, 1L, "CS301", "Data Structures", 4, 3, 201L));
        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, true, true));
        when(sessionRepository.save(any(AttendanceSession.class))).thenReturn(session);

        AttendanceSessionResponse response = sessionService.createSession(createRequest, "Bearer test-token");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getSubjectId());
        assertEquals("Room-302", response.getRoomNumber());
        verify(sessionRepository, times(1)).save(any(AttendanceSession.class));
    }

    @Test
    @DisplayName("Create session throws BadRequestException when start time is after end time")
    void testCreateSessionInvalidTime() {
        createRequest.setStartTime(LocalTime.of(11, 0));
        createRequest.setEndTime(LocalTime.of(9, 0));

        assertThrows(BadRequestException.class, () ->
                sessionService.createSession(createRequest, "Bearer test-token"));
        verifyNoInteractions(sessionRepository);
    }

    @Test
    @DisplayName("Create session throws ResourceNotFoundException when subject does not exist")
    void testCreateSessionSubjectNotFound() {
        when(academicServiceClient.getSubjectById(eq(10L), any())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.createSession(createRequest, "Bearer test-token"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create session throws BadRequestException when faculty user is invalid")
    void testCreateSessionInvalidFaculty() {
        when(academicServiceClient.getSubjectById(eq(10L), any()))
                .thenReturn(new SubjectResponseDto(10L, 1L, "CS301", "Data Structures", 4, 3, 201L));
        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, false, false));

        assertThrows(BadRequestException.class, () ->
                sessionService.createSession(createRequest, "Bearer test-token"));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get session by ID success")
    void testGetSessionByIdSuccess() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        AttendanceSessionResponse response = sessionService.getSessionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getSubjectId());
    }

    @Test
    @DisplayName("Get session by ID throws ResourceNotFoundException")
    void testGetSessionByIdNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sessionService.getSessionById(99L));
    }

    @Test
    @DisplayName("List sessions paginated")
    void testListSessions() {
        Page<AttendanceSession> page = new PageImpl<>(List.of(session));
        when(sessionRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AttendanceSessionResponse> result = sessionService.listSessions(
                10L, 201L, null, "2026-2027", 3, "A", PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Update session successfully")
    void testUpdateSessionSuccess() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(authServiceClient.validateUser(eq(201L), eq("FACULTY"), any()))
                .thenReturn(new UserValidationResponseDto(201L, true, true));
        when(sessionRepository.save(any(AttendanceSession.class))).thenReturn(session);

        AttendanceSessionResponse response = sessionService.updateSession(1L, updateRequest, "Bearer test-token");

        assertNotNull(response);
        verify(sessionRepository, times(1)).save(session);
    }

    @Test
    @DisplayName("Delete session successfully")
    void testDeleteSessionSuccess() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        sessionService.deleteSession(1L);

        verify(sessionRepository, times(1)).delete(session);
    }
}
