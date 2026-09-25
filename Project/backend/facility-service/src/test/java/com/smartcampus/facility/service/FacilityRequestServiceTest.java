package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequestStatusRequest;
import com.smartcampus.facility.dto.response.FacilityRequestResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityRequest;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.DuplicateResourceException;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.FacilityRequestRepository;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.impl.FacilityRequestServiceImpl;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityRequestServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityRequestRepository facilityRequestRepository;

    @InjectMocks
    private FacilityRequestServiceImpl facilityRequestService;

    private Facility facility;
    private FacilityRequest facilityRequest;
    private UserPrincipal studentPrincipal;
    private UserPrincipal anotherStudentPrincipal;
    private UserPrincipal adminPrincipal;
    private UserPrincipal facultyPrincipal;

    @BeforeEach
    void setUp() {
        facility = new Facility(
                "Seminar Hall A",
                FacilityType.SEMINAR_HALL,
                "Equipped with audio system",
                "Academic Block",
                "SH-1",
                120,
                FacilityStatus.AVAILABLE
        );
        facility.setId(1L);

        facilityRequest = new FacilityRequest(
                facility,
                10L, // student user ID
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Student Club Meeting",
                FacilityRequestStatus.PENDING
        );
        facilityRequest.setId(100L);

        studentPrincipal = new UserPrincipal(10L, "student1", "student1@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        anotherStudentPrincipal = new UserPrincipal(20L, "student2", "student2@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        facultyPrincipal = new UserPrincipal(5L, "faculty", "faculty@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));
    }

    @Test
    @DisplayName("Create request - Success (requester is authenticated user)")
    void createRequest_Success() {
        CreateFacilityBookingRequest request = new CreateFacilityBookingRequest(
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Student Club Meeting"
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRequestRepository.existsConflictingApprovedRequest(eq(1L), any(LocalDate.class), eq(FacilityRequestStatus.APPROVED), any(LocalTime.class), any(LocalTime.class), isNull()))
                .thenReturn(false);
        when(facilityRequestRepository.save(any(FacilityRequest.class))).thenReturn(facilityRequest);

        FacilityRequestResponse response = facilityRequestService.createRequest(1L, request, studentPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getRequestedByUserId());
        assertEquals(FacilityRequestStatus.PENDING, response.getStatus());
        verify(facilityRequestRepository).save(any(FacilityRequest.class));
    }

    @Test
    @DisplayName("Create request - Facility Unavailable throws BadRequestException")
    void createRequest_FacilityUnavailable() {
        facility.setStatus(FacilityStatus.MAINTENANCE);
        CreateFacilityBookingRequest request = new CreateFacilityBookingRequest(
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Meeting"
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));

        assertThrows(BadRequestException.class, () -> facilityRequestService.createRequest(1L, request, studentPrincipal));
    }

    @Test
    @DisplayName("Create request - Invalid time range throws BadRequestException")
    void createRequest_InvalidTimeRange() {
        CreateFacilityBookingRequest request = new CreateFacilityBookingRequest(
                LocalDate.now().plusDays(2),
                LocalTime.of(14, 0),
                LocalTime.of(12, 0), // end before start
                "Meeting"
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));

        assertThrows(BadRequestException.class, () -> facilityRequestService.createRequest(1L, request, studentPrincipal));
    }

    @Test
    @DisplayName("Create request - Overlapping approved booking throws DuplicateResourceException")
    void createRequest_Conflict() {
        CreateFacilityBookingRequest request = new CreateFacilityBookingRequest(
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Meeting"
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRequestRepository.existsConflictingApprovedRequest(eq(1L), any(LocalDate.class), eq(FacilityRequestStatus.APPROVED), any(LocalTime.class), any(LocalTime.class), isNull()))
                .thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> facilityRequestService.createRequest(1L, request, studentPrincipal));
    }

    @Test
    @DisplayName("Get requests by facility - Success")
    void getRequestsByFacilityId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByFacilityId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(facilityRequest), pageable, 1));

        Page<FacilityRequestResponse> result = facilityRequestService.getRequestsByFacilityId(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Get request by ID - Student own request success")
    void getRequestById_StudentOwnRequest_Success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        FacilityRequestResponse response = facilityRequestService.getRequestById(1L, 100L, studentPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Get request by ID - Student accessing another student's request throws AccessDeniedException")
    void getRequestById_StudentOtherRequest_ThrowsForbidden() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        assertThrows(AccessDeniedException.class, () -> facilityRequestService.getRequestById(1L, 100L, anotherStudentPrincipal));
    }

    @Test
    @DisplayName("Get request by ID - Admin access another user's request success")
    void getRequestById_AdminAccess_Success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        FacilityRequestResponse response = facilityRequestService.getRequestById(1L, 100L, adminPrincipal);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Update request - Student update own pending request success")
    void updateRequest_StudentOwnPending_Success() {
        UpdateFacilityBookingRequest request = new UpdateFacilityBookingRequest(
                LocalDate.now().plusDays(3),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                "Updated Purpose",
                "Updated Remarks"
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));
        when(facilityRequestRepository.save(any(FacilityRequest.class))).thenReturn(facilityRequest);

        FacilityRequestResponse response = facilityRequestService.updateRequest(1L, 100L, request, studentPrincipal);

        assertNotNull(response);
        assertEquals("Updated Purpose", facilityRequest.getPurpose());
    }

    @Test
    @DisplayName("Update request - Student update approved request throws BadRequestException")
    void updateRequest_StudentApproved_ThrowsBadRequest() {
        facilityRequest.setStatus(FacilityRequestStatus.APPROVED);
        UpdateFacilityBookingRequest request = new UpdateFacilityBookingRequest(
                LocalDate.now().plusDays(3),
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                "Updated Purpose",
                null
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        assertThrows(BadRequestException.class, () -> facilityRequestService.updateRequest(1L, 100L, request, studentPrincipal));
    }

    @Test
    @DisplayName("Update request status - Student forbidden")
    void updateRequestStatus_Student_ThrowsForbidden() {
        UpdateFacilityRequestStatusRequest request = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Approved by self"
        );

        assertThrows(AccessDeniedException.class, () -> facilityRequestService.updateRequestStatus(1L, 100L, request, studentPrincipal));
    }

    @Test
    @DisplayName("Update request status - Faculty approval success (sets approvedByUserId)")
    void updateRequestStatus_FacultyApprove_Success() {
        UpdateFacilityRequestStatusRequest request = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Approved by department"
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));
        when(facilityRequestRepository.existsConflictingApprovedRequest(eq(1L), any(LocalDate.class), eq(FacilityRequestStatus.APPROVED), any(LocalTime.class), any(LocalTime.class), eq(100L)))
                .thenReturn(false);
        when(facilityRequestRepository.save(any(FacilityRequest.class))).thenReturn(facilityRequest);

        FacilityRequestResponse response = facilityRequestService.updateRequestStatus(1L, 100L, request, facultyPrincipal);

        assertNotNull(response);
        assertEquals(FacilityRequestStatus.APPROVED, facilityRequest.getStatus());
        assertEquals(5L, facilityRequest.getApprovedByUserId());
    }

    @Test
    @DisplayName("Update request status - Illegal transition throws BadRequestException")
    void updateRequestStatus_IllegalTransition_ThrowsBadRequest() {
        facilityRequest.setStatus(FacilityRequestStatus.REJECTED);
        UpdateFacilityRequestStatusRequest request = new UpdateFacilityRequestStatusRequest(
                FacilityRequestStatus.APPROVED, "Re-approve"
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        assertThrows(BadRequestException.class, () -> facilityRequestService.updateRequestStatus(1L, 100L, request, adminPrincipal));
    }

    @Test
    @DisplayName("Cancel request - Student cancel own request success")
    void cancelRequest_StudentOwn_Success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(facilityRequestRepository.findByIdAndFacilityId(100L, 1L)).thenReturn(Optional.of(facilityRequest));

        facilityRequestService.cancelOrDeleteRequest(1L, 100L, studentPrincipal);

        assertEquals(FacilityRequestStatus.CANCELLED, facilityRequest.getStatus());
        verify(facilityRequestRepository).save(facilityRequest);
    }
}
