package com.smartcampus.facility.service.impl;

import com.smartcampus.facility.dto.request.CreateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequestStatusRequest;
import com.smartcampus.facility.dto.response.FacilityRequestResponse;
import com.smartcampus.facility.entity.Facility;
import com.smartcampus.facility.entity.FacilityRequest;
import com.smartcampus.facility.entity.FacilityRequestStatus;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.DuplicateResourceException;
import com.smartcampus.facility.exception.ResourceNotFoundException;
import com.smartcampus.facility.repository.FacilityRepository;
import com.smartcampus.facility.repository.FacilityRequestRepository;
import com.smartcampus.facility.security.UserPrincipal;
import com.smartcampus.facility.service.FacilityRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FacilityRequestServiceImpl implements FacilityRequestService {

    private static final Logger log = LoggerFactory.getLogger(FacilityRequestServiceImpl.class);

    private final FacilityRepository facilityRepository;
    private final FacilityRequestRepository facilityRequestRepository;

    public FacilityRequestServiceImpl(FacilityRepository facilityRepository,
                                      FacilityRequestRepository facilityRequestRepository) {
        this.facilityRepository = facilityRepository;
        this.facilityRequestRepository = facilityRequestRepository;
    }

    @Override
    public FacilityRequestResponse createRequest(Long facilityId, CreateFacilityBookingRequest request, UserPrincipal principal) {
        log.info("Creating facility request for facilityId={}, userId={}", facilityId, principal.getUserId());

        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));

        if (facility.getStatus() != FacilityStatus.AVAILABLE) {
            throw new BadRequestException("Facility " + facility.getName() + " is currently " + facility.getStatus() + " and cannot be requested");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        boolean hasConflict = facilityRequestRepository.existsConflictingApprovedRequest(
                facilityId,
                request.getRequestDate(),
                FacilityRequestStatus.APPROVED,
                request.getStartTime(),
                request.getEndTime(),
                null
        );

        if (hasConflict) {
            throw new DuplicateResourceException("Facility is already booked for the requested time slot on " + request.getRequestDate());
        }

        FacilityRequest facilityRequest = new FacilityRequest(
                facility,
                principal.getUserId(),
                request.getRequestDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getPurpose().trim(),
                FacilityRequestStatus.PENDING
        );

        FacilityRequest saved = facilityRequestRepository.save(facilityRequest);
        log.info("Facility request created successfully with ID: {}", saved.getId());
        return FacilityRequestResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacilityRequestResponse> getRequestsByFacilityId(Long facilityId, Pageable pageable) {
        log.info("Fetching requests for facilityId: {}", facilityId);
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        return facilityRequestRepository.findByFacilityId(facilityId, pageable)
                .map(FacilityRequestResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public FacilityRequestResponse getRequestById(Long facilityId, Long requestId, UserPrincipal principal) {
        log.info("Fetching request ID: {} for facilityId: {} by user: {}", requestId, facilityId, principal.getUserId());
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        FacilityRequest request = facilityRequestRepository.findByIdAndFacilityId(requestId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility request not found with ID: " + requestId + " for facility ID: " + facilityId));

        if (isStudentOnly(principal) && !request.getRequestedByUserId().equals(principal.getUserId())) {
            log.warn("Access denied: Student {} attempted to access request {} belonging to user {}",
                    principal.getUserId(), requestId, request.getRequestedByUserId());
            throw new AccessDeniedException("You are not authorized to view this facility request");
        }

        return FacilityRequestResponse.fromEntity(request);
    }

    @Override
    public FacilityRequestResponse updateRequest(Long facilityId, Long requestId, UpdateFacilityBookingRequest request, UserPrincipal principal) {
        log.info("Updating request ID: {} for facilityId: {} by user: {}", requestId, facilityId, principal.getUserId());
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        FacilityRequest entity = facilityRequestRepository.findByIdAndFacilityId(requestId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility request not found with ID: " + requestId + " for facility ID: " + facilityId));

        if (isStudentOnly(principal)) {
            if (!entity.getRequestedByUserId().equals(principal.getUserId())) {
                throw new AccessDeniedException("You are not authorized to update this facility request");
            }
            if (entity.getStatus() != FacilityRequestStatus.PENDING) {
                throw new BadRequestException("Students can only modify requests in PENDING status. Current status: " + entity.getStatus());
            }
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        if (entity.getStatus() == FacilityRequestStatus.APPROVED) {
            boolean hasConflict = facilityRequestRepository.existsConflictingApprovedRequest(
                    facilityId,
                    request.getRequestDate(),
                    FacilityRequestStatus.APPROVED,
                    request.getStartTime(),
                    request.getEndTime(),
                    requestId
            );
            if (hasConflict) {
                throw new DuplicateResourceException("Facility is already booked for the requested time slot on " + request.getRequestDate());
            }
        }

        entity.setRequestDate(request.getRequestDate());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setPurpose(request.getPurpose().trim());
        if (request.getRemarks() != null) {
            entity.setRemarks(request.getRemarks().trim());
        }

        FacilityRequest updated = facilityRequestRepository.save(entity);
        return FacilityRequestResponse.fromEntity(updated);
    }

    @Override
    public FacilityRequestResponse updateRequestStatus(Long facilityId, Long requestId, UpdateFacilityRequestStatusRequest request, UserPrincipal principal) {
        log.info("Updating status of request ID: {} for facilityId: {} to {} by user: {}",
                requestId, facilityId, request.getStatus(), principal.getUserId());

        if (isStudentOnly(principal)) {
            throw new AccessDeniedException("Students are not permitted to change request status");
        }

        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        FacilityRequest entity = facilityRequestRepository.findByIdAndFacilityId(requestId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility request not found with ID: " + requestId + " for facility ID: " + facilityId));

        FacilityRequestStatus currentStatus = entity.getStatus();
        FacilityRequestStatus newStatus = request.getStatus();

        if (newStatus == null) {
            throw new BadRequestException("Status must not be null");
        }

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == FacilityRequestStatus.APPROVED) {
            boolean hasConflict = facilityRequestRepository.existsConflictingApprovedRequest(
                    facilityId,
                    entity.getRequestDate(),
                    FacilityRequestStatus.APPROVED,
                    entity.getStartTime(),
                    entity.getEndTime(),
                    requestId
            );
            if (hasConflict) {
                throw new DuplicateResourceException("Cannot approve request: conflicting approved booking exists for this time slot");
            }
            entity.setApprovedByUserId(principal.getUserId());
        }

        entity.setStatus(newStatus);
        if (request.getRemarks() != null) {
            entity.setRemarks(request.getRemarks().trim());
        }

        FacilityRequest updated = facilityRequestRepository.save(entity);
        return FacilityRequestResponse.fromEntity(updated);
    }

    @Override
    public void cancelOrDeleteRequest(Long facilityId, Long requestId, UserPrincipal principal) {
        log.info("Cancelling/Deleting request ID: {} for facilityId: {} by user: {}", requestId, facilityId, principal.getUserId());
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with ID: " + facilityId);
        }

        FacilityRequest entity = facilityRequestRepository.findByIdAndFacilityId(requestId, facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility request not found with ID: " + requestId + " for facility ID: " + facilityId));

        if (isStudentOnly(principal) && !entity.getRequestedByUserId().equals(principal.getUserId())) {
            throw new AccessDeniedException("You are not authorized to cancel this facility request");
        }

        if (entity.getStatus() == FacilityRequestStatus.COMPLETED || entity.getStatus() == FacilityRequestStatus.CANCELLED) {
            throw new BadRequestException("Request is already in " + entity.getStatus() + " state");
        }

        entity.setStatus(FacilityRequestStatus.CANCELLED);
        facilityRequestRepository.save(entity);
        log.info("Facility request ID {} successfully marked as CANCELLED", requestId);
    }

    private void validateStatusTransition(FacilityRequestStatus currentStatus, FacilityRequestStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == FacilityRequestStatus.APPROVED
                    || newStatus == FacilityRequestStatus.REJECTED
                    || newStatus == FacilityRequestStatus.CANCELLED;
            case APPROVED -> newStatus == FacilityRequestStatus.COMPLETED
                    || newStatus == FacilityRequestStatus.CANCELLED;
            case REJECTED, CANCELLED, COMPLETED -> false;
        };

        if (!isValid) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }
    }

    private boolean isStudentOnly(UserPrincipal principal) {
        boolean hasStudent = false;
        boolean hasAdminOrFaculty = false;
        for (GrantedAuthority authority : principal.getAuthorities()) {
            String auth = authority.getAuthority();
            if ("ROLE_ADMIN".equals(auth) || "ROLE_FACULTY".equals(auth)) {
                hasAdminOrFaculty = true;
            }
            if ("ROLE_STUDENT".equals(auth)) {
                hasStudent = true;
            }
        }
        return hasStudent && !hasAdminOrFaculty;
    }
}
