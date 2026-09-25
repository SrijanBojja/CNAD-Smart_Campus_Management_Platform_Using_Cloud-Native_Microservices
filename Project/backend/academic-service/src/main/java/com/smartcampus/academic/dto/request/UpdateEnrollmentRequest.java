package com.smartcampus.academic.dto.request;

import com.smartcampus.academic.entity.EnrollmentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateEnrollmentRequest {

    @NotBlank(message = "Academic year is required")
    @Size(min = 4, max = 20, message = "Academic year must be between 4 and 20 characters (e.g. '2026-2027')")
    private String academicYear;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester cannot exceed 12")
    private Integer semester;

    @NotNull(message = "Enrollment status is required")
    private EnrollmentStatus status;

    public UpdateEnrollmentRequest() {
    }

    public UpdateEnrollmentRequest(String academicYear, Integer semester, EnrollmentStatus status) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.status = status;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
