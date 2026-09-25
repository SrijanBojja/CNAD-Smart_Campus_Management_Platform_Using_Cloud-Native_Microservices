package com.smartcampus.facility.service.impl;

import com.smartcampus.facility.dto.request.CreateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceRecordRequest;
import com.smartcampus.facility.dto.request.UpdateMaintenanceStatusRequest;
import com.smartcampus.facility.dto.response.MaintenanceRecordResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.MaintenancePriority;
import com.smartcampus.facility.entity.MaintenanceRecord;
import com.smartcampus.facility.entity.MaintenanceStatus;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.MaintenanceRecordRepository;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.MaintenanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class MaintenanceRecordServiceImpl implements MaintenanceRecordService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceRecordServiceImpl.class);

    private final FacilityRepository facilityRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceRecordServiceImpl(FacilityRepository facilityRepository,
                                        MaintenanceRecordRepository maintenanceRecordRepository) {
        this.facilityRepository = facilityRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Override
    public MaintenanceRecordResponse createMaintenanceRecord(Long facilityId, CreateMaintenanceRecordRequest request, UserPrincipal principal) {
        log.info("Creating maintenance record for facilityId={}, reporterId={}", facilityId, principal.getUserId());

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));

        MaintenancePriority priority = request.getPriority() != null ? request.getPriority() : MaintenancePriority.MEDIUM;
        MaintenanceRecord record = new MaintenanceRecord(
                facility,
                principal.getUserId(),
                request.getIssueDescription().trim(),
                priority,
                MaintenanceStatus.OPEN
        );
        record.setAssignedToUserId(request.getAssignedToUserId());

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        log.info("Maintenance record created successfully with ID: {}", saved.getId());
        return MaintenanceRecordResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRecordResponse> getMaintenanceRecordsByFacilityId(Long facilityId, Pageable pageable) {
        log.info("Fetching maintenance records for facilityId: {}", facilityId);
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        return maintenanceRecordRepository.findByFacilityId(facilityId, pageable)
                .map(MaintenanceRecordResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRecordResponse getMaintenanceRecordById(Long facilityId, Long maintenanceId) {
        log.info("Fetching maintenance record ID: {} for facilityId: {}", maintenanceId, facilityId);
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndFacilityId(maintenanceId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with ID: " + maintenanceId + " for facility ID: " + facilityId));

        return MaintenanceRecordResponse.fromEntity(record);
    }

    @Override
    public MaintenanceRecordResponse updateMaintenanceRecord(Long facilityId, Long maintenanceId, UpdateMaintenanceRecordRequest request) {
        log.info("Updating maintenance record ID: {} for facilityId: {}", maintenanceId, facilityId);
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndFacilityId(maintenanceId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with ID: " + maintenanceId + " for facility ID: " + facilityId));

        record.setIssueDescription(request.getIssueDescription().trim());
        record.setPriority(request.getPriority());
        record.setAssignedToUserId(request.getAssignedToUserId());
        if (request.getResolutionNotes() != null) {
            record.setResolutionNotes(request.getResolutionNotes().trim());
        }

        MaintenanceRecord updated = maintenanceRecordRepository.save(record);
        return MaintenanceRecordResponse.fromEntity(updated);
    }

    @Override
    public MaintenanceRecordResponse updateMaintenanceStatus(Long facilityId, Long maintenanceId, UpdateMaintenanceStatusRequest request) {
        log.info("Updating maintenance status for ID: {} in facilityId: {} to {}", maintenanceId, facilityId, request.getStatus());
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndFacilityId(maintenanceId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with ID: " + maintenanceId + " for facility ID: " + facilityId));

        MaintenanceStatus currentStatus = record.getStatus();
        MaintenanceStatus newStatus = request.getStatus();

        if (newStatus == null) {
            throw new BadRequestException("Maintenance status must not be null");
        }

        validateStatusTransition(currentStatus, newStatus);

        record.setStatus(newStatus);
        if (request.getAssignedToUserId() != null) {
            record.setAssignedToUserId(request.getAssignedToUserId());
        }
        if (request.getResolutionNotes() != null) {
            record.setResolutionNotes(request.getResolutionNotes().trim());
        }

        if (newStatus == MaintenanceStatus.RESOLVED) {
            record.setResolvedAt(LocalDateTime.now());
        } else if (currentStatus == MaintenanceStatus.RESOLVED && newStatus != MaintenanceStatus.RESOLVED) {
            record.setResolvedAt(null);
        }

        MaintenanceRecord updated = maintenanceRecordRepository.save(record);
        return MaintenanceRecordResponse.fromEntity(updated);
    }

    @Override
    public void cancelOrDeleteMaintenanceRecord(Long facilityId, Long maintenanceId) {
        log.info("Cancelling maintenance record ID: {} for facilityId: {}", maintenanceId, facilityId);
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        MaintenanceRecord record = maintenanceRecordRepository.findByIdAndFacilityId(maintenanceId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with ID: " + maintenanceId + " for facility ID: " + facilityId));

        if (record.getStatus() == MaintenanceStatus.CANCELLED) {
            throw new BadRequestException("Maintenance record is already cancelled");
        }

        record.setStatus(MaintenanceStatus.CANCELLED);
        maintenanceRecordRepository.save(record);
        log.info("Maintenance record ID {} successfully marked as CANCELLED", maintenanceId);
    }

    private void validateStatusTransition(MaintenanceStatus currentStatus, MaintenanceStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case OPEN -> newStatus == MaintenanceStatus.IN_PROGRESS
                    || newStatus == MaintenanceStatus.RESOLVED
                    || newStatus == MaintenanceStatus.CANCELLED;
            case IN_PROGRESS -> newStatus == MaintenanceStatus.RESOLVED
                    || newStatus == MaintenanceStatus.CANCELLED
                    || newStatus == MaintenanceStatus.OPEN;
            case RESOLVED -> newStatus == MaintenanceStatus.OPEN
                    || newStatus == MaintenanceStatus.IN_PROGRESS;
            case CANCELLED -> false;
        };

        if (!isValid) {
            throw new BadRequestException("Invalid maintenance status transition from " + currentStatus + " to " + newStatus);
        }
    }
}
