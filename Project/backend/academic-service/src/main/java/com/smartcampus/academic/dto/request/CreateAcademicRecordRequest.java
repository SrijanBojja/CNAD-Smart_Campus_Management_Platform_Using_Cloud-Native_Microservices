package com.smartcampus.academic.dto.request;

import com.smartcampus.academic.entity.ResultStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateAcademicRecordRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotBlank(message = "Academic year is required")
    @Size(min = 4, max = 20, message = "Academic year must be between 4 and 20 characters")
    private String academicYear;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester cannot exceed 12")
    private Integer semester;

    @DecimalMin(value = "0.00", message = "Internal marks cannot be negative")
    @DecimalMax(value = "100.00", message = "Internal marks cannot exceed 100.00")
    private BigDecimal internalMarks;

    @DecimalMin(value = "0.00", message = "External marks cannot be negative")
    @DecimalMax(value = "100.00", message = "External marks cannot exceed 100.00")
    private BigDecimal externalMarks;

    @Size(max = 5, message = "Grade must not exceed 5 characters")
    private String grade;

    @DecimalMin(value = "0.00", message = "Grade point cannot be negative")
    @DecimalMax(value = "10.00", message = "Grade point cannot exceed 10.00")
    private BigDecimal gradePoint;

    private ResultStatus resultStatus;

    public CreateAcademicRecordRequest() {
    }

    public CreateAcademicRecordRequest(Long studentId, Long subjectId, String academicYear, Integer semester,
                                       BigDecimal internalMarks, BigDecimal externalMarks, String grade,
                                       BigDecimal gradePoint, ResultStatus resultStatus) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.resultStatus = resultStatus;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
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

    public BigDecimal getInternalMarks() {
        return internalMarks;
    }

    public void setInternalMarks(BigDecimal internalMarks) {
        this.internalMarks = internalMarks;
    }

    public BigDecimal getExternalMarks() {
        return externalMarks;
    }

    public void setExternalMarks(BigDecimal externalMarks) {
        this.externalMarks = externalMarks;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public BigDecimal getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(BigDecimal gradePoint) {
        this.gradePoint = gradePoint;
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }
}
