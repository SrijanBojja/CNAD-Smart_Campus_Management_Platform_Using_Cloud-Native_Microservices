package com.smartcampus.academic.dto.request;

import com.smartcampus.academic.entity.CourseStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateCourseStatusRequest {

    @NotNull(message = "Course status is required")
    private CourseStatus status;

    public UpdateCourseStatusRequest() {
    }

    public UpdateCourseStatusRequest(CourseStatus status) {
        this.status = status;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public void setStatus(CourseStatus status) {
        this.status = status;
    }
}
