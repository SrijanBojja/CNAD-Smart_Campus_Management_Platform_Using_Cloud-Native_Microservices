package com.smartcampus.attendance.service;

import com.smartcampus.attendance.client.StudentServiceClient;
import com.smartcampus.attendance.client.dto.StudentResponseDto;
import com.smartcampus.attendance.dto.request.CreateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.request.UpdateAttendanceRecordRequest;
import com.smartcampus.attendance.dto.response.AttendanceRecordResponse;
import com.smartcampus.attendance.entity.AttendanceRecord;
import com.smartcampus.attendance.entity.AttendanceSession;
import com.smartcampus.attendance.entity.AttendanceStatus;
import com.smartcampus.attendance.exception.DuplicateResourceException;
import com.smartcampus.attendance.exception.ResourceNotFoundException;
import com.smartcampus.attendance.repository.AttendanceRecordRepository;
import com.smartcampus.attendance.repository.AttendanceSessionRepository;
import com.smartcampus.attendance.security.UserPrincipal;
import com.smartcampus.attendance.service.impl.AttendanceRecordServiceImpl;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordServiceTest {

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @InjectMocks
    private AttendanceRecordServiceImpl recordService;

    private AttendanceSession session;
    private AttendanceRecord record;
    private CreateAttendanceRecordRequest createRequest;
    private UpdateAttendanceRecordRequest updateRequest;

    @BeforeEach
    void setUp() {
        session = new AttendanceSession(
                10L, 201L, LocalDate.of(2026, 9, 25),
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Room-302",
                "2026-2027", 3, "A"
        );
        session.setId(1L);

        record = new AttendanceRecord(session, 50L, AttendanceStatus.PRESENT, "On time");
        record.setId(100L);

        createRequest = new CreateAttendanceRecordRequest(1L, 50L, AttendanceStatus.PRESENT, "On time");
        updateRequest = new UpdateAttendanceRecordRequest(AttendanceStatus.EXCUSED, "Medical leave approved");
    }

    @Test
    @DisplayName("Create attendance record successfully")
    void testCreateRecordSuccess() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));
        when(recordRepository.existsBySessionIdAndStudentId(1L, 50L)).thenReturn(false);
        when(recordRepository.save(any(AttendanceRecord.class))).thenReturn(record);

        AttendanceRecordResponse response = recordService.createRecord(createRequest, "Bearer test-token");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getSessionId());
        assertEquals(50L, response.getStudentId());
        assertEquals(AttendanceStatus.PRESENT, response.getStatus());
        verify(recordRepository, times(1)).save(any(AttendanceRecord.class));
    }

    @Test
    @DisplayName("Create attendance record throws DuplicateResourceException on duplicate")
    void testCreateRecordDuplicate() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));
        when(recordRepository.existsBySessionIdAndStudentId(1L, 50L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                recordService.createRecord(createRequest, "Bearer test-token"));
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create attendance record throws ResourceNotFoundException when student does not exist")
    void testCreateRecordStudentNotFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(studentServiceClient.getStudentById(eq(50L), any())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () ->
                recordService.createRecord(createRequest, "Bearer test-token"));
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get attendance record by ID as ADMIN succeeds")
    void testGetRecordAsAdmin() {
        when(recordRepository.findById(100L)).thenReturn(Optional.of(record));
        UserPrincipal adminPrincipal = new UserPrincipal(999L, "admin", "admin@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        AttendanceRecordResponse response = recordService.getRecordById(100L, adminPrincipal, "Bearer test-token");

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Get attendance record by ID as owner STUDENT succeeds")
    void testGetRecordAsOwnerStudent() {
        when(recordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));

        UserPrincipal studentPrincipal = new UserPrincipal(101L, "alice", "alice@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        AttendanceRecordResponse response = recordService.getRecordById(100L, studentPrincipal, "Bearer test-token");

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Get attendance record by ID as another STUDENT throws AccessDeniedException")
    void testGetRecordAsOtherStudentDenied() {
        when(recordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(studentServiceClient.getStudentById(eq(50L), any()))
                .thenReturn(new StudentResponseDto(50L, 101L, "STU2026001", "Alice", "Smith", "ACTIVE"));

        UserPrincipal otherStudentPrincipal = new UserPrincipal(102L, "bob", "bob@campus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        assertThrows(AccessDeniedException.class, () ->
                recordService.getRecordById(100L, otherStudentPrincipal, "Bearer test-token"));
    }

    @Test
    @DisplayName("List attendance records paginated")
    void testListRecords() {
        Page<AttendanceRecord> page = new PageImpl<>(List.of(record));
        when(recordRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AttendanceRecordResponse> result = recordService.listRecords(1L, 50L, AttendanceStatus.PRESENT, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Update attendance record successfully")
    void testUpdateRecordSuccess() {
        when(recordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(recordRepository.save(any(AttendanceRecord.class))).thenReturn(record);

        AttendanceRecordResponse response = recordService.updateRecord(100L, updateRequest);

        assertNotNull(response);
        assertEquals(AttendanceStatus.EXCUSED, record.getStatus());
        verify(recordRepository, times(1)).save(record);
    }

    @Test
    @DisplayName("Delete attendance record successfully")
    void testDeleteRecordSuccess() {
        when(recordRepository.findById(100L)).thenReturn(Optional.of(record));

        recordService.deleteRecord(100L);

        verify(recordRepository, times(1)).delete(record);
    }
}
