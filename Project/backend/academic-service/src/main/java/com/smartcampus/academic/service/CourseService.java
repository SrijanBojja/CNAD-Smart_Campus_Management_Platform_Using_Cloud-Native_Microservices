package com.smartcampus.academic.service;

import com.smartcampus.academic.dto.request.CreateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseRequest;
import com.smartcampus.academic.dto.request.UpdateCourseStatusRequest;
import com.smartcampus.academic.dto.response.CourseResponse;
import com.smartcampus.academic.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest request);

    CourseResponse getCourseById(Long id);

    Page<CourseResponse> listCourses(String department, CourseStatus status, Pageable pageable);

    CourseResponse updateCourse(Long id, UpdateCourseRequest request);

    CourseResponse updateCourseStatus(Long id, UpdateCourseStatusRequest request);
}
