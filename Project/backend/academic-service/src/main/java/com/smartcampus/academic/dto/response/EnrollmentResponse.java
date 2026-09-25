package com.smartcampus.academic.dto.response;

import com.smartcampus.academic.entity.CourseEnrollment;
import com.smartcampus.academic.entity.EnrollmentStatus;
import java.time.LocalDateTime;

public class EnrollmentResponse {

    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long studentId;
    private String academicYear;
    private Integer semester;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EnrollmentResponse() {
    }

    public EnrollmentResponse(Long id, Long courseId, String courseCode, String courseName,
                              Long studentId, String academicYear, Integer semester,
                              EnrollmentStatus status, LocalDateTime enrolledAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.studentId = studentId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EnrollmentResponse fromEntity(CourseEnrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse() != null ? enrollment.getCourse().getId() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getCourseCode() : null,
                enrollment.getCourse() != null ? enrollment.getCourse().getCourseName() : null,
                enrollment.getStudentId(),
                enrollment.getAcademicYear(),
                enrollment.getSemester(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
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

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
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
