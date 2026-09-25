package com.smartcampus.academic.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
    name = "academic_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_record_unique", columnNames = {"student_id", "subject_id", "academic_year", "semester"})
    }
)
public class AcademicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "internal_marks", precision = 5, scale = 2)
    private BigDecimal internalMarks;

    @Column(name = "external_marks", precision = 5, scale = 2)
    private BigDecimal externalMarks;

    @Column(name = "total_marks", precision = 5, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "grade", length = 5)
    private String grade;

    @Column(name = "grade_point", precision = 4, scale = 2)
    private BigDecimal gradePoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", length = 20)
    private ResultStatus resultStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AcademicRecord() {
    }

    public AcademicRecord(Long studentId, Subject subject, String academicYear, Integer semester,
                          BigDecimal internalMarks, BigDecimal externalMarks, BigDecimal totalMarks,
                          String grade, BigDecimal gradePoint, ResultStatus resultStatus) {
        this.studentId = studentId;
        this.subject = subject;
        this.academicYear = academicYear;
        this.semester = semester;
        this.internalMarks = internalMarks;
        this.externalMarks = externalMarks;
        this.totalMarks = totalMarks;
        this.grade = grade;
        this.gradePoint = gradePoint;
        this.resultStatus = resultStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        calculateTotalAndStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotalAndStatus();
    }

    public void calculateTotalAndStatus() {
        if (internalMarks != null && externalMarks != null) {
            this.totalMarks = internalMarks.add(externalMarks);
        } else if (internalMarks != null) {
            this.totalMarks = internalMarks;
        } else if (externalMarks != null) {
            this.totalMarks = externalMarks;
        }
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

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicRecord that = (AcademicRecord) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(studentId, that.studentId) &&
                 Objects.equals(subject, that.subject) &&
                 Objects.equals(academicYear, that.academicYear) &&
                 Objects.equals(semester, that.semester));
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, subject, academicYear, semester);
    }
}
