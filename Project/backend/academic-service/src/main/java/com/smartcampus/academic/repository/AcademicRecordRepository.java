package com.smartcampus.academic.repository;

import com.smartcampus.academic.entity.AcademicRecord;
import com.smartcampus.academic.entity.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {

    boolean existsByStudentIdAndSubjectIdAndAcademicYearAndSemester(Long studentId, Long subjectId, String academicYear, Integer semester);

    Page<AcademicRecord> findByStudentId(Long studentId, Pageable pageable);

    Page<AcademicRecord> findBySubjectId(Long subjectId, Pageable pageable);

    Page<AcademicRecord> findByStudentIdAndSemester(Long studentId, Integer semester, Pageable pageable);

    Page<AcademicRecord> findByStudentIdAndAcademicYear(Long studentId, String academicYear, Pageable pageable);

    Page<AcademicRecord> findByResultStatus(ResultStatus resultStatus, Pageable pageable);

    List<AcademicRecord> findByStudentId(Long studentId);
}
