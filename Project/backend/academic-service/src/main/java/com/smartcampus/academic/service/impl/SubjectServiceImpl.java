package com.smartcampus.academic.service.impl;

import com.smartcampus.academic.client.AuthServiceClient;
import com.smartcampus.academic.dto.request.CreateSubjectRequest;
import com.smartcampus.academic.dto.request.UpdateSubjectRequest;
import com.smartcampus.academic.dto.response.SubjectResponse;
import com.smartcampus.academic.entity.Course;
import com.smartcampus.academic.entity.Subject;
import com.smartcampus.academic.exception.DuplicateResourceException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.repository.CourseRepository;
import com.smartcampus.academic.repository.SubjectRepository;
import com.smartcampus.academic.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectServiceImpl implements SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectServiceImpl.class);

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final AuthServiceClient authServiceClient;

    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              CourseRepository courseRepository,
                              AuthServiceClient authServiceClient) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        log.info("Attempting to create subject with code: {}", request.getSubjectCode());

        if (subjectRepository.existsBySubjectCode(request.getSubjectCode().trim())) {
            log.warn("Subject creation failed: subjectCode '{}' already exists", request.getSubjectCode());
            throw new DuplicateResourceException("Subject with code '" + request.getSubjectCode() + "' already exists");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + request.getCourseId()));

        Subject subject = new Subject(
                course,
                request.getSubjectCode().trim().toUpperCase(),
                request.getSubjectName().trim(),
                request.getCredits(),
                request.getSemester(),
                request.getFacultyUserId()
        );

        Subject saved = subjectRepository.save(subject);
        log.info("Subject successfully created with ID: {}, code: {}", saved.getId(), saved.getSubjectCode());

        return SubjectResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {
        log.debug("Retrieving subject by ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        return SubjectResponse.fromEntity(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubjectResponse> listSubjects(Long courseId, Integer semester, Long facultyUserId, Pageable pageable) {
        log.debug("Listing subjects with courseId: {}, semester: {}, facultyUserId: {}", courseId, semester, facultyUserId);
        Page<Subject> page;
        if (courseId != null && semester != null) {
            page = subjectRepository.findByCourseIdAndSemester(courseId, semester, pageable);
        } else if (courseId != null) {
            page = subjectRepository.findByCourseId(courseId, pageable);
        } else if (semester != null) {
            page = subjectRepository.findBySemester(semester, pageable);
        } else if (facultyUserId != null) {
            page = subjectRepository.findByFacultyUserId(facultyUserId, pageable);
        } else {
            page = subjectRepository.findAll(pageable);
        }
        return page.map(SubjectResponse::fromEntity);
    }

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long id, UpdateSubjectRequest request) {
        log.info("Updating subject with ID: {}", id);

        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        subject.setSubjectName(request.getSubjectName().trim());
        subject.setCredits(request.getCredits());
        subject.setSemester(request.getSemester());
        subject.setFacultyUserId(request.getFacultyUserId());

        Subject updated = subjectRepository.save(subject);
        log.info("Subject successfully updated with ID: {}", updated.getId());

        return SubjectResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        log.info("Deleting subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));
        subjectRepository.delete(subject);
        log.info("Subject successfully deleted with ID: {}", id);
    }
}
