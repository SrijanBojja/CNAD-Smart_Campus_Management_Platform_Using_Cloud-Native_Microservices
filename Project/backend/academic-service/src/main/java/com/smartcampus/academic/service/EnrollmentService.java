package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateEnrollmentRequest;
import com.smartcampus.academic.dto.request.UpdateEnrollmentRequest;
import com.smartcampus.academic.dto.response.EnrollmentResponse;
import com.smartcampus.academic.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {

    EnrollmentResponse createEnrollment(CreateEnrollmentRequest request, String bearerToken);

    EnrollmentResponse getEnrollmentById(Long id, UserPrincipal principal, String bearerToken);

    Page<EnrollmentResponse> listEnrollments(Long studentId, Long courseId, String academicYear, Integer semester, Pageable pageable);

    EnrollmentResponse updateEnrollment(Long id, UpdateEnrollmentRequest request);

    void deleteEnrollment(Long id);
}
