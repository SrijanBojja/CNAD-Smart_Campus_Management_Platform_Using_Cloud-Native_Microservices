package com.smartcampus.academic.dto.request;

import com.smartcampus.academic.entity.ResultStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateAcademicRecordRequest {

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

    public UpdateAcademicRecordRequest() {
    }

    public UpdateAcademicRecordRequest(BigDecimal internalMarks, BigDecimal externalMarks, String grade,
                                       BigDecimal gradePoint, ResultStatus resultStatus) {
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.resultStatus = resultStatus;
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
