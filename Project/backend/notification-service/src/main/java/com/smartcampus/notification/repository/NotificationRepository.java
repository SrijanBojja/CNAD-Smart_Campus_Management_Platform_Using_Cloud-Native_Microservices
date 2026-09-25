package com.smartcampus.notification.repository;

import com.smartcampus.notification.entity.Notification;
import com.smartcampus.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Page<Notification> findByRecipientUserId(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndStatus(Long recipientUserId, NotificationStatus status);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);
}
