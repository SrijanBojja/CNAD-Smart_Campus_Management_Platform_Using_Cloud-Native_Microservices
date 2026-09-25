package com.smartcampus.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Payload for creating a new attendance session")
public class CreateAttendanceSessionRequest {

    @NotNull(message = "subjectId is required")
    @Positive(message = "subjectId must be positive")
    @Schema(description = "Subject ID referenced from Academic Service", example = "10")
    private Long subjectId;

    @NotNull(message = "facultyUserId is required")
    @Positive(message = "facultyUserId must be positive")
    @Schema(description = "Faculty user ID referenced from Auth Service", example = "201")
    private Long facultyUserId;

    @NotNull(message = "sessionDate is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Date of the attendance session", example = "2026-09-25")
    private LocalDate sessionDate;

    @NotNull(message = "startTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    @Schema(description = "Session start time", example = "09:00:00")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    @Schema(description = "Session end time", example = "10:30:00")
    private LocalTime endTime;

    @Size(max = 50, message = "roomNumber cannot exceed 50 characters")
    @Schema(description = "Room number or lecture hall", example = "Room-302")
    private String roomNumber;

    @NotBlank(message = "academicYear is required")
    @Size(max = 20, message = "academicYear cannot exceed 20 characters")
    @Schema(description = "Academic year", example = "2026-2027")
    private String academicYear;

    @NotNull(message = "semester is required")
    @Positive(message = "semester must be positive")
    @Schema(description = "Semester number", example = "3")
    private Integer semester;

    @Size(max = 20, message = "section cannot exceed 20 characters")
    @Schema(description = "Class section", example = "A")
    private String section;

    public CreateAttendanceSessionRequest() {
    }

    public CreateAttendanceSessionRequest(Long subjectId, Long facultyUserId, LocalDate sessionDate,
                                         LocalTime startTime, LocalTime endTime, String roomNumber,
                                         String academicYear, Integer semester, String section) {
        this.subjectId = subjectId;
        this.facultyUserId = facultyUserId;
        this.sessionDate = sessionDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.roomNumber = roomNumber;
        this.academicYear = academicYear;
        this.semester = semester;
        this.section = section;
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
}
