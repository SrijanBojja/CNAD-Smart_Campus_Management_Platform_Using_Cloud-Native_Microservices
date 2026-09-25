package com.smartcampus.event.service;

import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.request.UpdateEventRequest;
import com.smartcampus.event.dto.response.EventResponse;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface EventService {

    EventResponse createEvent(CreateEventRequest request, UserPrincipal principal, String bearerToken);

    EventResponse getEventById(Long id);

    Page<EventResponse> listEvents(EventType eventType, EventStatus status, Long organizerUserId,
                                  LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    EventResponse updateEvent(Long id, UpdateEventRequest request, UserPrincipal principal, String bearerToken);

    void deleteOrCancelEvent(Long id, UserPrincipal principal);
}
