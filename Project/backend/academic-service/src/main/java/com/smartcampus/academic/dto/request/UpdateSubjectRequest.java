package com.smartcampus.academic.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateSubjectRequest {

    @NotBlank(message = "Subject name is required")
    @Size(min = 2, max = 150, message = "Subject name must be between 2 and 150 characters")
    private String subjectName;

    @NotNull(message = "Credits are required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 30, message = "Credits cannot exceed 30")
    private Integer credits;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester cannot exceed 12")
    private Integer semester;

    private Long facultyUserId;

    public UpdateSubjectRequest() {
    }

    public UpdateSubjectRequest(String subjectName, Integer credits, Integer semester, Long facultyUserId) {
        this.subjectName = subjectName;
        this.credits = credits;
        this.semester = semester;
        this.facultyUserId = facultyUserId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Long getFacultyUserId() {
        return facultyUserId;
    }

    public void setFacultyUserId(Long facultyUserId) {
        this.facultyUserId = facultyUserId;
    }
}
