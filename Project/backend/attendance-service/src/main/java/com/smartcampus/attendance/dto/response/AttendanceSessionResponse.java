package com.smartcampus.attendance.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartcampus.attendance.entity.AttendanceSession;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "Attendance session details response")
public class AttendanceSessionResponse {

    @Schema(description = "Attendance session ID", example = "1")
    private Long id;

    @Schema(description = "Subject ID referenced from Academic Service", example = "10")
    private Long subjectId;

    @Schema(description = "Faculty user ID referenced from Auth Service", example = "201")
    private Long facultyUserId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the session", example = "2026-09-25")
    private LocalDate sessionDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    @Schema(description = "Session start time", example = "09:00:00")
    private LocalTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    @Schema(description = "Session end time", example = "10:30:00")
    private LocalTime endTime;

    @Schema(description = "Room number", example = "Room-302")
    private String roomNumber;

    @Schema(description = "Academic year", example = "2026-2027")
    private String academicYear;

    @Schema(description = "Semester number", example = "3")
    private Integer semester;

    @Schema(description = "Section", example = "A")
    private String section;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Record creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Record last update timestamp")
    private LocalDateTime updatedAt;

    public AttendanceSessionResponse() {
    }

    public static AttendanceSessionResponse fromEntity(AttendanceSession entity) {
        if (entity == null) {
            return null;
        }
        AttendanceSessionResponse dto = new AttendanceSessionResponse();
        dto.setId(entity.getId());
        dto.setSubjectId(entity.getSubjectId());
        dto.setFacultyUserId(entity.getFacultyUserId());
        dto.setSessionDate(entity.getSessionDate());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setRoomNumber(entity.getRoomNumber());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setSemester(entity.getSemester());
        dto.setSection(entity.getSection());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
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

    public Long getFacultyUserId() {
        return facultyUserId;
    }

    public void setFacultyUserId(Long facultyUserId) {
        this.facultyUserId = facultyUserId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
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
