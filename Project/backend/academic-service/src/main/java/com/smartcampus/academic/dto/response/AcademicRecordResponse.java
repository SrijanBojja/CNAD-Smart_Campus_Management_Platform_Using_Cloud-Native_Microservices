package com.smartcampus.academic.dto.response;

import com.smartcampus.academic.entity.AcademicRecord;
import com.smartcampus.academic.entity.ResultStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AcademicRecordResponse {

    private Long id;
    private Long studentId;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private String academicYear;
    private Integer semester;
    private BigDecimal internalMarks;
    private BigDecimal externalMarks;
    private BigDecimal totalMarks;
    private String grade;
    private BigDecimal gradePoint;
    private ResultStatus resultStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AcademicRecordResponse() {
    }

    public AcademicRecordResponse(Long id, Long studentId, Long subjectId, String subjectCode,
                                  String subjectName, String academicYear, Integer semester,
                                  BigDecimal internalMarks, BigDecimal externalMarks,
                                  BigDecimal totalMarks, String grade, BigDecimal gradePoint,
                                  ResultStatus resultStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.academicYear = academicYear;
        this.semester = semester;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.totalMarks = totalMarks;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.resultStatus = resultStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AcademicRecordResponse fromEntity(AcademicRecord record) {
        return new AcademicRecordResponse(
                record.getId(),
                record.getStudentId(),
                record.getSubject() != null ? record.getSubject().getId() : null,
                record.getSubject() != null ? record.getSubject().getSubjectCode() : null,
                record.getSubject() != null ? record.getSubject().getSubjectName() : null,
                record.getAcademicYear(),
                record.getSemester(),
                record.getInternalMarks(),
                record.getExternalMarks(),
                record.getTotalMarks(),
                record.getGrade(),
                record.getGradePoint(),
                record.getResultStatus(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
