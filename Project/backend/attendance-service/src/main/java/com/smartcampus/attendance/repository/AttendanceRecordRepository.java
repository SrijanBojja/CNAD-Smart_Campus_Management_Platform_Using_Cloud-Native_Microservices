package com.smartcampus.attendance.repository;

import com.smartcampus.attendance.entity.AttendanceRecord;
import com.smartcampus.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>, JpaSpecificationExecutor<AttendanceRecord> {

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    Page<AttendanceRecord> findBySessionId(Long sessionId, Pageable pageable);

    Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);

    Page<AttendanceRecord> findBySessionIdAndStatus(Long sessionId, AttendanceStatus status, Pageable pageable);

    Page<AttendanceRecord> findByStudentIdAndStatus(Long studentId, AttendanceStatus status, Pageable pageable);

    List<AttendanceRecord> findBySessionId(Long sessionId);
}
