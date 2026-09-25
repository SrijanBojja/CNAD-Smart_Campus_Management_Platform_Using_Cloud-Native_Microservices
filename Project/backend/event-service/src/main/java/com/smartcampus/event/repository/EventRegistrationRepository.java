package com.smartcampus.event.repository;

import com.smartcampus.event.entity.EventRegistration;
import com.smartcampus.event.entity.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long>, JpaSpecificationExecutor<EventRegistration> {

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    Page<EventRegistration> findByEventId(Long eventId, Pageable pageable);

    Page<EventRegistration> findByEventIdAndStatus(Long eventId, RegistrationStatus status, Pageable pageable);

    Page<EventRegistration> findByUserId(Long userId, Pageable pageable);

    Optional<EventRegistration> findByIdAndEventId(Long id, Long eventId);
}
