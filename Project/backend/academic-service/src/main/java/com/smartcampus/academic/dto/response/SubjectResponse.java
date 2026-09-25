package com.smartcampus.academic.dto.response;

import com.smartcampus.academic.entity.Subject;
import java.time.LocalDateTime;

public class SubjectResponse {

    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Integer semester;
    private Long facultyUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SubjectResponse() {
    }

    public SubjectResponse(Long id, Long courseId, String courseCode, String courseName,
                           String subjectCode, String subjectName, Integer credits,
                           Integer semester, Long facultyUserId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.semester = semester;
        this.facultyUserId = facultyUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SubjectResponse fromEntity(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getCourse() != null ? subject.getCourse().getId() : null,
                subject.getCourse() != null ? subject.getCourse().getCourseCode() : null,
                subject.getCourse() != null ? subject.getCourse().getCourseName() : null,
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getCredits(),
                subject.getSemester(),
                subject.getFacultyUserId(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
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
