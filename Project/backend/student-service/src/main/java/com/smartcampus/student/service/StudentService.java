package com.smartcampus.student.service;

import com.smartcampus.student.dto.request.CreateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentRequest;
import com.smartcampus.student.dto.request.UpdateStudentStatusRequest;
import com.smartcampus.student.dto.response.StudentResponse;
import com.smartcampus.student.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    StudentResponse createStudent(CreateStudentRequest request, String bearerToken);
    StudentResponse getStudentById(Long id, UserPrincipal principal);
    Page<StudentResponse> listStudents(Pageable pageable);
    StudentResponse updateStudent(Long id, UpdateStudentRequest request);
    StudentResponse updateStudentStatus(Long id, UpdateStudentStatusRequest request);
}
