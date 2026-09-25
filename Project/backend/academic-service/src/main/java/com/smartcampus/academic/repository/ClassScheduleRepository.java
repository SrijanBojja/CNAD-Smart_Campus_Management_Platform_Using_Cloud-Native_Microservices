package com.smartcampus.academic.repository;

import com.smartcampus.academic.entity.ClassSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    Page<ClassSchedule> findBySubjectId(Long subjectId, Pageable pageable);

    Page<ClassSchedule> findByFacultyUserId(Long facultyUserId, Pageable pageable);

    Page<ClassSchedule> findByDayOfWeek(String dayOfWeek, Pageable pageable);

    Page<ClassSchedule> findByAcademicYearAndSemester(String academicYear, Integer semester, Pageable pageable);

    Page<ClassSchedule> findBySubjectIdAndSection(Long subjectId, String section, Pageable pageable);

    List<ClassSchedule> findByFacultyUserId(Long facultyUserId);
}
