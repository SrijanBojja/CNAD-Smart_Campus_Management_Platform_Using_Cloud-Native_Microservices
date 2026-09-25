package com.smartcampus.attendance.client.dto;

public class SubjectResponseDto {
    private Long id;
    private Long courseId;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Integer semester;
    private Long facultyUserId;

    public SubjectResponseDto() {
    }

    public SubjectResponseDto(Long id, Long courseId, String subjectCode, String subjectName, Integer credits, Integer semester, Long facultyUserId) {
        this.id = id;
        this.courseId = courseId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.credits = credits;
        this.semester = semester;
        this.facultyUserId = facultyUserId;
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
}
