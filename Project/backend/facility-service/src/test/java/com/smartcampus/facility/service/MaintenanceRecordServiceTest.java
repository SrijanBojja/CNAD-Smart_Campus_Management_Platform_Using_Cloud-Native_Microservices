package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceStatusRequest;
import com.smartcampus.facility.dto.response.MaintenanceRecordResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.entity.MaintenancePriority;
import com.smartcampus.facility.entity.MaintenanceRecord;
import com.smartcampus.facility.entity.MaintenanceStatus;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.MaintenanceRecordRepository;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.impl.MaintenanceRecordServiceImpl;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceRecordServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @InjectMocks
    private MaintenanceRecordServiceImpl maintenanceRecordService;

    private Facility facility;
    private MaintenanceRecord maintenanceRecord;
    private UserPrincipal facultyPrincipal;

    @BeforeEach
    void setUp() {
        facility = new Facility(
                "Auditorium",
                FacilityType.AUDITORIUM,
                "Campus Auditorium",
                "Central Building",
                "AUD-1",
                500,
                FacilityStatus.AVAILABLE
        );
        facility.setId(1L);

        maintenanceRecord = new MaintenanceRecord(
                facility,
                5L, // faculty user ID
                "Audio amplifier humming loudly",
                MaintenancePriority.HIGH,
                MaintenanceStatus.OPEN
        );
        maintenanceRecord.setId(200L);

        facultyPrincipal = new UserPrincipal(5L, "faculty1", "faculty1@smartcampus.edu",
                List.of(new SimpleGrantedAuthority("ROLE_FACULTY")));
    }

    @Test
    @DisplayName("Create maintenance record - Success")
    void createMaintenanceRecord_Success() {
        CreateMaintenanceRecordRequest request = new CreateMaintenanceRecordRequest(
                "Audio amplifier humming loudly",
                MaintenancePriority.HIGH,
                15L
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenReturn(maintenanceRecord);

        MaintenanceRecordResponse response = maintenanceRecordService.createMaintenanceRecord(1L, request, facultyPrincipal);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals(5L, response.getReportedByUserId());
        assertEquals(MaintenanceStatus.OPEN, response.getStatus());
        verify(maintenanceRecordRepository).save(any(MaintenanceRecord.class));
    }

    @Test
    @DisplayName("Get maintenance records by facility - Success")
    void getMaintenanceRecordsByFacilityId_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByFacilityId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(maintenanceRecord), pageable, 1));

        Page<MaintenanceRecordResponse> result = maintenanceRecordService.getMaintenanceRecordsByFacilityId(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Get maintenance record by ID - Success")
    void getMaintenanceRecordById_Success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByIdAndFacilityId(200L, 1L)).thenReturn(Optional.of(maintenanceRecord));

        MaintenanceRecordResponse response = maintenanceRecordService.getMaintenanceRecordById(1L, 200L);

        assertNotNull(response);
        assertEquals(200L, response.getId());
        assertEquals("Audio amplifier humming loudly", response.getIssueDescription());
    }

    @Test
    @DisplayName("Update maintenance record - Success")
    void updateMaintenanceRecord_Success() {
        UpdateMaintenanceRecordRequest request = new UpdateMaintenanceRecordRequest(
                "Audio amplifier humming loudly - replaced ground cable",
                MaintenancePriority.MEDIUM,
                15L,
                "Diagnosed grounding fault"
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByIdAndFacilityId(200L, 1L)).thenReturn(Optional.of(maintenanceRecord));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenReturn(maintenanceRecord);

        MaintenanceRecordResponse response = maintenanceRecordService.updateMaintenanceRecord(1L, 200L, request);

        assertNotNull(response);
        assertEquals("Audio amplifier humming loudly - replaced ground cable", maintenanceRecord.getIssueDescription());
    }

    @Test
    @DisplayName("Update maintenance status - Transition to RESOLVED sets resolvedAt")
    void updateMaintenanceStatus_ToResolved_Success() {
        UpdateMaintenanceStatusRequest request = new UpdateMaintenanceStatusRequest(
                MaintenanceStatus.RESOLVED,
                15L,
                "Replaced fuse and tested"
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByIdAndFacilityId(200L, 1L)).thenReturn(Optional.of(maintenanceRecord));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenReturn(maintenanceRecord);

        MaintenanceRecordResponse response = maintenanceRecordService.updateMaintenanceStatus(1L, 200L, request);

        assertNotNull(response);
        assertEquals(MaintenanceStatus.RESOLVED, maintenanceRecord.getStatus());
        assertNotNull(maintenanceRecord.getResolvedAt());
    }

    @Test
    @DisplayName("Update maintenance status - Illegal transition from CANCELLED throws BadRequestException")
    void updateMaintenanceStatus_FromCancelled_ThrowsBadRequest() {
        maintenanceRecord.setStatus(MaintenanceStatus.CANCELLED);
        UpdateMaintenanceStatusRequest request = new UpdateMaintenanceStatusRequest(
                MaintenanceStatus.RESOLVED,
                null,
                null
        );

        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByIdAndFacilityId(200L, 1L)).thenReturn(Optional.of(maintenanceRecord));

        assertThrows(BadRequestException.class, () -> maintenanceRecordService.updateMaintenanceStatus(1L, 200L, request));
    }

    @Test
    @DisplayName("Cancel maintenance record - Sets status to CANCELLED")
    void cancelOrDeleteMaintenanceRecord_Success() {
        when(facilityRepository.existsById(1L)).thenReturn(true);
        when(maintenanceRecordRepository.findByIdAndFacilityId(200L, 1L)).thenReturn(Optional.of(maintenanceRecord));

        maintenanceRecordService.cancelOrDeleteMaintenanceRecord(1L, 200L);

        assertEquals(MaintenanceStatus.CANCELLED, maintenanceRecord.getStatus());
        verify(maintenanceRecordRepository).save(maintenanceRecord);
    }
}
