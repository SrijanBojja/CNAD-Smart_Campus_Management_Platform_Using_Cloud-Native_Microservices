package com.smartcampus.event.repository;

import com.smartcampus.event.entity.Event;
import com.smartcampus.event.entity.EventStatus;
import com.smartcampus.event.entity.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByEventType(EventType eventType, Pageable pageable);

    Page<Event> findByOrganizerUserId(Long organizerUserId, Pageable pageable);

    List<Event> findByOrganizerUserId(Long organizerUserId);
}
