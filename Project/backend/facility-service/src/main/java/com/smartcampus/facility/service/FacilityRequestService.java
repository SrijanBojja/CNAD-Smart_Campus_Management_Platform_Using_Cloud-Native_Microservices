package com.smartcampus.facility.service;

import com.smartcampus.facility.dto.request.CreateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityBookingRequest;
import com.smartcampus.facility.dto.request.UpdateFacilityRequestStatusRequest;
import com.smartcampus.facility.dto.response.FacilityRequestResponse;
import com.smartcampus.facility.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FacilityRequestService {

    FacilityRequestResponse createRequest(Long facilityId, CreateFacilityBookingRequest request, UserPrincipal principal);

    Page<FacilityRequestResponse> getRequestsByFacilityId(Long facilityId, Pageable pageable);

    FacilityRequestResponse getRequestById(Long facilityId, Long requestId, UserPrincipal principal);

    FacilityRequestResponse updateRequest(Long facilityId, Long requestId, UpdateFacilityBookingRequest request, UserPrincipal principal);

    FacilityRequestResponse updateRequestStatus(Long facilityId, Long requestId, UpdateFacilityRequestStatusRequest request, UserPrincipal principal);

    void cancelOrDeleteRequest(Long facilityId, Long requestId, UserPrincipal principal);
}
