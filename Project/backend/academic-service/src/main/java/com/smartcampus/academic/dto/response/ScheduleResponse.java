package com.smartcampus.academic.dto.response;

import com.smartcampus.academic.entity.ClassSchedule;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ScheduleResponse {

    private Long id;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Long facultyUserId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String roomNumber;
    private String academicYear;
    private Integer semester;
    private String section;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduleResponse() {
    }

    public ScheduleResponse(Long id, Long subjectId, String subjectCode, String subjectName,
                            Long facultyUserId, String dayOfWeek, LocalTime startTime,
                            LocalTime endTime, String roomNumber, String academicYear,
                            Integer semester, String section, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.facultyUserId = facultyUserId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
        this.academicYear = academicYear;
        this.semester = semester;
        this.section = section;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ScheduleResponse fromEntity(ClassSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getSubject() != null ? schedule.getSubject().getId() : null,
                schedule.getSubject() != null ? schedule.getSubject().getSubjectCode() : null,
                schedule.getSubject() != null ? schedule.getSubject().getSubjectName() : null,
                schedule.getFacultyUserId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRoomNumber(),
                schedule.getAcademicYear(),
                schedule.getSemester(),
                schedule.getSection(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getFacultyUserId() {
        return facultyUserId;
    }

    public void setFacultyUserId(Long facultyUserId) {
        this.facultyUserId = facultyUserId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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
