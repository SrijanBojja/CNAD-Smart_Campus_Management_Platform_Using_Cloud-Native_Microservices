package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityStatusRequest;
import com.smartcampus.facility.dto.response.FacilityResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.FacilityRequestRepository;
import com.smartcampus.facility.repository.MaintenanceRecordRepository;
import com.smartcampus.facility.service.impl.FacilityServiceImpl;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityRequestRepository facilityRequestRepository;

    @Mock
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @InjectMocks
    private FacilityServiceImpl facilityService;

    private Facility facility;

    @BeforeEach
    void setUp() {
        facility = new Facility(
                "CS Lab 101",
                FacilityType.LAB,
                "Advanced AI Computing Lab",
                "Technology Block",
                "TB-101",
                40,
                FacilityStatus.AVAILABLE
        );
        facility.setId(1L);
    }

    @Test
    @DisplayName("Create facility - Success")
    void createFacility_Success() {
        CreateFacilityRequest request = new CreateFacilityRequest(
                "CS Lab 101",
                FacilityType.LAB,
                "Advanced AI Computing Lab",
                "Technology Block",
                "TB-101",
                40,
                FacilityStatus.AVAILABLE
        );

        when(facilityRepository.save(any(Facility.class))).thenReturn(facility);

        FacilityResponse response = facilityService.createFacility(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CS Lab 101", response.getName());
        assertEquals(FacilityType.LAB, response.getFacilityType());
        verify(facilityRepository).save(any(Facility.class));
    }

    @Test
    @DisplayName("Get facility by ID - Success")
    void getFacilityById_Success() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));

        FacilityResponse response = facilityService.getFacilityById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CS Lab 101", response.getName());
    }

    @Test
    @DisplayName("Get facility by ID - Not Found")
    void getFacilityById_NotFound() {
        when(facilityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> facilityService.getFacilityById(999L));
    }

    @Test
    @DisplayName("Get all facilities with filters")
    void getAllFacilities_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Facility> page = new PageImpl<>(List.of(facility), pageable, 1);

        when(facilityRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<FacilityResponse> result = facilityService.getAllFacilities(FacilityType.LAB, "Technology", FacilityStatus.AVAILABLE, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CS Lab 101", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("Update facility - Success")
    void updateFacility_Success() {
        UpdateFacilityRequest request = new UpdateFacilityRequest(
                "CS Lab 101 - Updated",
                FacilityType.LAB,
                "Updated description",
                "Technology Block",
                "TB-101",
                50,
                FacilityStatus.AVAILABLE
        );

        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRepository.save(any(Facility.class))).thenReturn(facility);

        FacilityResponse response = facilityService.updateFacility(1L, request);

        assertNotNull(response);
        assertEquals("CS Lab 101 - Updated", facility.getName());
        assertEquals(50, facility.getCapacity());
    }

    @Test
    @DisplayName("Update facility status - Success")
    void updateFacilityStatus_Success() {
        UpdateFacilityStatusRequest request = new UpdateFacilityStatusRequest(FacilityStatus.MAINTENANCE);
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRepository.save(any(Facility.class))).thenReturn(facility);

        FacilityResponse response = facilityService.updateFacilityStatus(1L, request);

        assertNotNull(response);
        assertEquals(FacilityStatus.MAINTENANCE, facility.getStatus());
    }

    @Test
    @DisplayName("Delete facility - Hard delete when no history exists")
    void deleteFacility_HardDelete_WhenNoHistory() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRequestRepository.countByFacilityId(1L)).thenReturn(0L);
        when(maintenanceRecordRepository.countByFacilityId(1L)).thenReturn(0L);

        facilityService.deleteFacility(1L);

        verify(facilityRepository).delete(facility);
        verify(facilityRepository, never()).save(any(Facility.class));
    }

    @Test
    @DisplayName("Delete facility - Safe retire when requests or maintenance exist")
    void deleteFacility_SafeRetire_WhenHistoryExists() {
        when(facilityRepository.findById(1L)).thenReturn(Optional.of(facility));
        when(facilityRequestRepository.countByFacilityId(1L)).thenReturn(2L);
        when(maintenanceRecordRepository.countByFacilityId(1L)).thenReturn(1L);

        facilityService.deleteFacility(1L);

        verify(facilityRepository, never()).delete(any(Facility.class));
        verify(facilityRepository).save(facility);
        assertEquals(FacilityStatus.UNAVAILABLE, facility.getStatus());
    }
}
