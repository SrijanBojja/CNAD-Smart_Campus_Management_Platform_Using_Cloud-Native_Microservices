package com.smartcampus.academic.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateCourseRequest {

    @NotBlank(message = "Course code is required")
    @Size(min = 2, max = 30, message = "Course code must be between 2 and 30 characters")
    private String courseCode;

    @NotBlank(message = "Course name is required")
    @Size(min = 2, max = 150, message = "Course name must be between 2 and 150 characters")
    private String courseName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @NotNull(message = "Duration in years is required")
    @Min(value = 1, message = "Duration must be at least 1 year")
    @Max(value = 10, message = "Duration cannot exceed 10 years")
    private Integer durationYears;

    public CreateCourseRequest() {
    }

    public CreateCourseRequest(String courseCode, String courseName, String description, String department, Integer durationYears) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.department = department;
        this.durationYears = durationYears;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getDurationYears() {
        return durationYears;
    }

    public void setDurationYears(Integer durationYears) {
        this.durationYears = durationYears;
    }
}
