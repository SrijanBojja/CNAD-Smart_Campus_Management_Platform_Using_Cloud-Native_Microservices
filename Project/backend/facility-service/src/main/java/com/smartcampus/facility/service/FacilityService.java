package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityStatusRequest;
import com.smartcampus.facility.dto.response.FacilityResponse;
import com.smartcampus.facility.entity.FacilityStatus;
import com.smartcampus.facility.entity.FacilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FacilityService {

    Page<FacilityResponse> getAllFacilities(FacilityType type, String building, FacilityStatus status, Pageable pageable);

    FacilityResponse getFacilityById(Long id);

    FacilityResponse createFacility(CreateFacilityRequest request);

    FacilityResponse updateFacility(Long id, UpdateFacilityRequest request);

    FacilityResponse updateFacilityStatus(Long id, UpdateFacilityStatusRequest request);

    void deleteFacility(Long id);
}
