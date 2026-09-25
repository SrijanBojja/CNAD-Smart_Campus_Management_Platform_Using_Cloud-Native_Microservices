package com.smartcampus.attendance.repository;

import com.smartcampus.attendance.entity.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long>, JpaSpecificationExecutor<AttendanceSession> {

    Page<AttendanceSession> findBySubjectId(Long subjectId, Pageable pageable);

    Page<AttendanceSession> findByFacultyUserId(Long facultyUserId, Pageable pageable);

    Page<AttendanceSession> findBySessionDate(LocalDate sessionDate, Pageable pageable);

    Page<AttendanceSession> findByAcademicYearAndSemester(String academicYear, Integer semester, Pageable pageable);

    Page<AttendanceSession> findBySubjectIdAndFacultyUserId(Long subjectId, Long facultyUserId, Pageable pageable);

    Page<AttendanceSession> findBySubjectIdAndSessionDate(Long subjectId, LocalDate sessionDate, Pageable pageable);

    List<AttendanceSession> findBySubjectIdAndAcademicYearAndSemester(Long subjectId, String academicYear, Integer semester);
}
