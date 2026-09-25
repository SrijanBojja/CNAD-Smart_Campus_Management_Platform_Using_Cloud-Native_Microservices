package com.smartcampus.event.service.impl;

import com.smartcampus.event.client.AuthServiceClient;
import com.smartcampus.event.client.dto.UserValidationResponseDto;
import com.smartcampus.event.dto.request.CreateEventRequest;
import com.smartcampus.event.dto.request.UpdateEventRequest;
import com.smartcampus.event.dto.response.EventResponse;
import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.exception.BadRequestException;
import com.smartcampus.event.exception.ResourceNotFoundException;
import com.smartcampus.event.repository.EventRegistrationRepository;
import com.smartcampus.event.repository.EventRepository;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final AuthServiceClient authServiceClient;

    public EventServiceImpl(EventRepository eventRepository,
                            EventRegistrationRepository registrationRepository,
                            AuthServiceClient authServiceClient) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, UserPrincipal principal, String bearerToken) {
        log.info("Attempting to create event: title='{}', type='{}', start={}",
                request.getTitle(), request.getEventType(), request.getStartDateTime());

        if (request.getStartDateTime().isAfter(request.getEndDateTime())
                || request.getStartDateTime().isEqual(request.getEndDateTime())) {
            throw new BadRequestException("Start date time must be before end date time");
        }

        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        Long organizerUserId;
        if (isAdmin && request.getOrganizerUserId() != null) {
            organizerUserId = request.getOrganizerUserId();
        } else if (principal != null) {
            organizerUserId = principal.getUserId();
        } else {
            organizerUserId = request.getOrganizerUserId();
        }

        if (organizerUserId == null) {
            throw new BadRequestException("Organizer user ID cannot be determined");
        }

        // Cross-service validation: verify organizer with Auth Service
        UserValidationResponseDto validation = authServiceClient.validateUser(organizerUserId, "FACULTY", bearerToken);
        if (validation == null || !validation.isActive()) {
            throw new BadRequestException("Organizer user with ID " + organizerUserId + " is invalid or inactive");
        }

        Event event = new Event(
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getEventType(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getLocation() != null ? request.getLocation().trim() : null,
                organizerUserId,
                request.getCapacity(),
                EventStatus.UPCOMING
        );

        Event saved = eventRepository.save(event);
        log.info("Event successfully created with ID: {}", saved.getId());

        return EventResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        log.debug("Retrieving event by ID: {}", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));
        return EventResponse.fromEntity(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(EventType eventType, EventStatus status, Long organizerUserId,
                                         LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        log.debug("Listing events: type={}, status={}, organizer={}, from={}, to={}",
                eventType, status, organizerUserId, fromDate, toDate);

        Specification<Event> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (eventType != null) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (organizerUserId != null) {
                predicates.add(cb.equal(root.get("organizerUserId"), organizerUserId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDateTime"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDateTime"), toDate));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Event> page = eventRepository.findAll(spec, pageable);
        return page.map(EventResponse::fromEntity);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request, UserPrincipal principal, String bearerToken) {
        log.info("Updating event with ID: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        // Ownership and Role Check
        if (principal != null) {
            boolean isAdmin = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (!isAdmin && !event.getOrganizerUserId().equals(principal.getUserId())) {
                log.warn("Access denied for user {} attempting to update event {} organized by {}",
                        principal.getUserId(), id, event.getOrganizerUserId());
                throw new AccessDeniedException("Access denied: You are only authorized to update events you organized");
            }
        }

        if (request.getStartDateTime().isAfter(request.getEndDateTime())
                || request.getStartDateTime().isEqual(request.getEndDateTime())) {
            throw new BadRequestException("Start date time must be before end date time");
        }

        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setEventType(request.getEventType());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        event.setCapacity(request.getCapacity());
        event.setStatus(request.getStatus());

        Event updated = eventRepository.save(event);
        log.info("Event successfully updated with ID: {}", updated.getId());

        return EventResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteOrCancelEvent(Long id, UserPrincipal principal) {
        log.info("Deleting or cancelling event with ID: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + id));

        // Ownership and Role Check
        if (principal != null) {
            boolean isAdmin = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (!isAdmin && !event.getOrganizerUserId().equals(principal.getUserId())) {
                log.warn("Access denied for user {} attempting to delete/cancel event {} organized by {}",
                        principal.getUserId(), id, event.getOrganizerUserId());
                throw new AccessDeniedException("Access denied: You are only authorized to delete or cancel events you organized");
            }
        }

        long activeRegistrations = registrationRepository.countByEventIdAndStatus(id, RegistrationStatus.REGISTERED);
        if (activeRegistrations > 0) {
            // Safe lifecycle handling: cancel rather than hard delete when attendees are registered
            log.info("Event {} has {} active registrations; setting status to CANCELLED", id, activeRegistrations);
            event.setStatus(EventStatus.CANCELLED);
            eventRepository.save(event);
        } else {
            eventRepository.delete(event);
            log.info("Event {} successfully deleted", id);
        }
    }
}
