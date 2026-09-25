package com.smartcampus.event.service.impl;

import com.smartcampus.event.dto.response.EventRegistrationResponse;
import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventRegistration;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.RegistrationStatus;
import com.smartcampus.event.exception.BadRequestException;
import com.smartcampus.event.exception.DuplicateResourceException;
import com.smartcampus.event.exception.ResourceNotFoundException;
import com.smartcampus.event.repository.EventRegistrationRepository;
import com.smartcampus.event.repository.EventRepository;
import com.smartcampus.event.security.UserPrincipal;
import com.smartcampus.event.service.EventRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(EventRegistrationServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;

    public EventRegistrationServiceImpl(EventRepository eventRepository,
                                        EventRegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Override
    @Transactional
    public EventRegistrationResponse registerForEvent(Long eventId, UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new BadRequestException("Authenticated user ID is required for registration");
        }
        Long userId = principal.getUserId();
        log.info("User {} attempting to register for event ID {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot register for a cancelled event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new BadRequestException("Cannot register for a completed event");
        }

        // Check if active registration already exists
        var existingOpt = registrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingOpt.isPresent()) {
            EventRegistration existing = existingOpt.get();
            if (existing.getStatus() == RegistrationStatus.REGISTERED) {
                log.warn("User {} already registered for event {}", userId, eventId);
                throw new DuplicateResourceException("User is already registered for this event");
            } else {
                // Check capacity before re-activating cancelled registration
                if (event.getCapacity() != null) {
                    long activeCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
                    if (activeCount >= event.getCapacity()) {
                        throw new BadRequestException("Event capacity of " + event.getCapacity() + " has been reached");
                    }
                }
                existing.setStatus(RegistrationStatus.REGISTERED);
                existing.setRegisteredAt(LocalDateTime.now());
                EventRegistration updated = registrationRepository.save(existing);
                log.info("User {} re-activated registration for event {}", userId, eventId);
                return EventRegistrationResponse.fromEntity(updated);
            }
        }

        // Check capacity
        if (event.getCapacity() != null) {
            long activeCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED);
            if (activeCount >= event.getCapacity()) {
                log.warn("Event {} capacity reached ({}/{})", eventId, activeCount, event.getCapacity());
                throw new BadRequestException("Event capacity of " + event.getCapacity() + " has been reached");
            }
        }

        EventRegistration registration = new EventRegistration(event, userId, RegistrationStatus.REGISTERED);
        EventRegistration saved = registrationRepository.save(registration);
        log.info("User {} successfully registered for event {} (registration ID: {})", userId, eventId, saved.getId());

        return EventRegistrationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventRegistrationResponse> listRegistrationsForEvent(Long eventId, RegistrationStatus status, Pageable pageable) {
        log.debug("Listing registrations for event ID: {}, status: {}", eventId, status);

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with ID: " + eventId);
        }

        Page<EventRegistration> page;
        if (status != null) {
            page = registrationRepository.findByEventIdAndStatus(eventId, status, pageable);
        } else {
            page = registrationRepository.findByEventId(eventId, pageable);
        }

        return page.map(EventRegistrationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public EventRegistrationResponse getRegistrationById(Long eventId, Long registrationId, UserPrincipal principal) {
        log.debug("Retrieving registration ID {} for event ID {}", registrationId, eventId);

        EventRegistration registration = registrationRepository.findByIdAndEventId(registrationId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with ID " + registrationId + " for event " + eventId));

        if (principal != null) {
            boolean isAdminOrFaculty = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(r -> "ROLE_ADMIN".equals(r) || "ROLE_FACULTY".equals(r));

            if (!isAdminOrFaculty && !registration.getUserId().equals(principal.getUserId())) {
                log.warn("Access denied for user {} attempting to access registration {} owned by user {}",
                        principal.getUserId(), registrationId, registration.getUserId());
                throw new AccessDeniedException("Access denied: You are only permitted to view your own registration");
            }
        }

        return EventRegistrationResponse.fromEntity(registration);
    }

    @Override
    @Transactional
    public EventRegistrationResponse cancelRegistration(Long eventId, Long registrationId, UserPrincipal principal) {
        log.info("Attempting to cancel registration ID {} for event ID {}", registrationId, eventId);

        EventRegistration registration = registrationRepository.findByIdAndEventId(registrationId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found with ID " + registrationId + " for event " + eventId));

        if (principal != null) {
            boolean isAdmin = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            boolean isOrganizer = registration.getEvent() != null
                    && registration.getEvent().getOrganizerUserId().equals(principal.getUserId());

            boolean isOwner = registration.getUserId().equals(principal.getUserId());

            if (!isAdmin && !isOrganizer && !isOwner) {
                log.warn("Access denied for user {} attempting to cancel registration {}",
                        principal.getUserId(), registrationId);
                throw new AccessDeniedException("Access denied: You are only permitted to cancel your own registration");
            }
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        EventRegistration updated = registrationRepository.save(registration);
        log.info("Registration ID {} successfully cancelled", registrationId);

        return EventRegistrationResponse.fromEntity(updated);
    }
}
