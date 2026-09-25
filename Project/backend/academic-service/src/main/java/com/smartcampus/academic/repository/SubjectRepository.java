package com.smartcampus.academic.repository;

import com.smartcampus.academic.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySubjectCode(String subjectCode);

    boolean existsBySubjectCode(String subjectCode);

    Page<Subject> findByCourseId(Long courseId, Pageable pageable);

    Page<Subject> findBySemester(Integer semester, Pageable pageable);

    Page<Subject> findByCourseIdAndSemester(Long courseId, Integer semester, Pageable pageable);

    Page<Subject> findByFacultyUserId(Long facultyUserId, Pageable pageable);

    List<Subject> findByCourseId(Long courseId);
}
