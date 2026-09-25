package com.smartcampus.facility.service.impl;

import com.smartcampus.facility.dto.request.CreateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityStatusRequest;
import com.smartcampus.facility.dto.response.FacilityResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.FacilityRequestRepository;
import com.smartcampus.facility.repository.MaintenanceRecordRepository;
import com.smartcampus.facility.service.FacilityService;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class FacilityServiceImpl implements FacilityService {

    private static final Logger log = LoggerFactory.getLogger(FacilityServiceImpl.class);

    private final FacilityRepository facilityRepository;
    private final FacilityRequestRepository facilityRequestRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository,
                               FacilityRequestRepository facilityRequestRepository,
                               MaintenanceRecordRepository maintenanceRecordRepository) {
        this.facilityRepository = facilityRepository;
        this.facilityRequestRepository = facilityRequestRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacilityResponse> getAllFacilities(FacilityType type, String building, FacilityStatus status, Pageable pageable) {
        log.info("Fetching facilities with filters: type={}, building={}, status={}, pageable={}", type, building, status, pageable);

        Specification<Facility> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("facilityType"), type));
            }
            if (building != null && !building.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("building")), "%" + building.trim().toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return facilityRepository.findAll(spec, pageable).map(FacilityResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(Long id) {
        log.info("Fetching facility by ID: {}", id);
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));
        return FacilityResponse.fromEntity(facility);
    }

    @Override
    public FacilityResponse createFacility(CreateFacilityRequest request) {
        log.info("Creating new facility: name={}, type={}", request.getName(), request.getFacilityType());

        FacilityStatus status = request.getStatus() != null ? request.getStatus() : FacilityStatus.AVAILABLE;
        Facility facility = new Facility(
                request.getName().trim(),
                request.getFacilityType(),
                request.getDescription(),
                request.getBuilding() != null ? request.getBuilding().trim() : null,
                request.getRoomNumber() != null ? request.getRoomNumber().trim() : null,
                request.getCapacity(),
                status
        );

        Facility saved = facilityRepository.save(facility);
        log.info("Successfully created facility with ID: {}", saved.getId());
        return FacilityResponse.fromEntity(saved);
    }

    @Override
    public FacilityResponse updateFacility(Long id, UpdateFacilityRequest request) {
        log.info("Updating facility with ID: {}", id);
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        facility.setName(request.getName().trim());
        facility.setFacilityType(request.getFacilityType());
        facility.setDescription(request.getDescription());
        facility.setBuilding(request.getBuilding() != null ? request.getBuilding().trim() : null);
        facility.setRoomNumber(request.getRoomNumber() != null ? request.getRoomNumber().trim() : null);
        facility.setCapacity(request.getCapacity());
        if (request.getStatus() != null) {
            facility.setStatus(request.getStatus());
        }

        Facility updated = facilityRepository.save(facility);
        log.info("Successfully updated facility with ID: {}", updated.getId());
        return FacilityResponse.fromEntity(updated);
    }

    @Override
    public FacilityResponse updateFacilityStatus(Long id, UpdateFacilityStatusRequest request) {
        log.info("Updating status for facility ID: {} to {}", id, request.getStatus());
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        if (request.getStatus() == null) {
            throw new BadRequestException("Status must not be null");
        }

        facility.setStatus(request.getStatus());
        Facility updated = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(updated);
    }

    @Override
    public void deleteFacility(Long id) {
        log.info("Attempting to delete facility with ID: {}", id);
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        long requestCount = facilityRequestRepository.countByFacilityId(id);
        long maintenanceCount = maintenanceRecordRepository.countByFacilityId(id);

        if (requestCount > 0 || maintenanceCount > 0) {
            log.info("Facility ID {} has associated requests ({}) or maintenance records ({}); updating status to UNAVAILABLE instead of physical delete",
                    id, requestCount, maintenanceCount);
            facility.setStatus(FacilityStatus.UNAVAILABLE);
            facilityRepository.save(facility);
        } else {
            log.info("Physically deleting facility ID {}", id);
            facilityRepository.delete(facility);
        }
    }
}
