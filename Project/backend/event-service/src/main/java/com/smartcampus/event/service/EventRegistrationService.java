package com.smartcampus.event.service;

import com.smartcampus.event.dto.response.EventRegistrationResponse;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRegistrationService {

    EventRegistrationResponse registerForEvent(Long eventId, UserPrincipal principal);

    Page<EventRegistrationResponse> listRegistrationsForEvent(Long eventId, RegistrationStatus status, Pageable pageable);

    EventRegistrationResponse getRegistrationById(Long eventId, Long registrationId, UserPrincipal principal);

    EventRegistrationResponse cancelRegistration(Long eventId, Long registrationId, UserPrincipal principal);
}
