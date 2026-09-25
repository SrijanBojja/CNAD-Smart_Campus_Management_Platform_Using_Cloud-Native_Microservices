package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateSubjectRequest;
import com.smartcampus.academic.dto.request.UpdateSubjectRequest;
import com.smartcampus.academic.dto.response.SubjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubjectService {

    SubjectResponse createSubject(CreateSubjectRequest request);

    SubjectResponse getSubjectById(Long id);

    Page<SubjectResponse> listSubjects(Long courseId, Integer semester, Long facultyUserId, Pageable pageable);

    SubjectResponse updateSubject(Long id, UpdateSubjectRequest request);

    void deleteSubject(Long id);
}
