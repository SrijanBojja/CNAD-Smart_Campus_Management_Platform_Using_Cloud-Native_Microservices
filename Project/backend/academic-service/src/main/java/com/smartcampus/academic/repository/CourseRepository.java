package com.smartcampus.academic.repository;

import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    Page<Course> findByDepartment(String department, Pageable pageable);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByDepartmentAndStatus(String department, CourseStatus status, Pageable pageable);
}
