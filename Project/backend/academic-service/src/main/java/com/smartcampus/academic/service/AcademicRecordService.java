package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateAcademicRecordRequest;
import com.smartcampus.academic.dto.request.UpdateAcademicRecordRequest;
import com.smartcampus.academic.dto.response.AcademicRecordResponse;
import com.smartcampus.academic.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AcademicRecordService {

    AcademicRecordResponse createAcademicRecord(CreateAcademicRecordRequest request, String bearerToken);

    AcademicRecordResponse getAcademicRecordById(Long id, UserPrincipal principal, String bearerToken);

    Page<AcademicRecordResponse> listAcademicRecords(Long studentId, Long subjectId, Integer semester, String academicYear, Pageable pageable);

    AcademicRecordResponse updateAcademicRecord(Long id, UpdateAcademicRecordRequest request);
}
