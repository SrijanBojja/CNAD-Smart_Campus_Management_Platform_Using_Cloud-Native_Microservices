package com.smartcampus.academic.repository;

import com.smartcampus.academic.entity.CourseEnrollment;
import com.smartcampus.academic.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByCourseIdAndStudentIdAndAcademicYearAndSemester(Long courseId, Long studentId, String academicYear, Integer semester);

    Page<CourseEnrollment> findByStudentId(Long studentId, Pageable pageable);

    Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);

    Page<CourseEnrollment> findByAcademicYearAndSemester(String academicYear, Integer semester, Pageable pageable);

    Page<CourseEnrollment> findByCourseIdAndAcademicYearAndSemester(Long courseId, String academicYear, Integer semester, Pageable pageable);

    Page<CourseEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);

    List<CourseEnrollment> findByStudentId(Long studentId);
}
