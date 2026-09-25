package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceStatusRequest;
import com.smartcampus.facility.dto.response.MaintenanceRecordResponse;
import com.smartcampus.facility.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceRecordService {

    MaintenanceRecordResponse createMaintenanceRecord(Long facilityId, CreateMaintenanceRecordRequest request, UserPrincipal principal);

    Page<MaintenanceRecordResponse> getMaintenanceRecordsByFacilityId(Long facilityId, Pageable pageable);

    MaintenanceRecordResponse getMaintenanceRecordById(Long facilityId, Long maintenanceId);

    MaintenanceRecordResponse updateMaintenanceRecord(Long facilityId, Long maintenanceId, UpdateMaintenanceRecordRequest request);

    MaintenanceRecordResponse updateMaintenanceStatus(Long facilityId, Long maintenanceId, UpdateMaintenanceStatusRequest request);

    void cancelOrDeleteMaintenanceRecord(Long facilityId, Long maintenanceId);
}
