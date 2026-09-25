package com.smartcampus.student.dto.response;

import com.smartcampus.student.entity.Student;
import com.smartcampus.student.entity.StudentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Response payload representing a student profile")
public class StudentResponse {

    @Schema(description = "Primary key ID of the student record", example = "1")
    private Long id;

    @Schema(description = "Referenced Auth user numeric ID", example = "101")
    private Long userId;

    @Schema(description = "Unique student registration number", example = "STU2026001")
    private String studentNumber;

    @Schema(description = "Student first name", example = "Srijan")
    private String firstName;

    @Schema(description = "Student last name", example = "Bojja")
    private String lastName;

    @Schema(description = "Date of birth (YYYY-MM-DD)", example = "2005-08-15")
    private LocalDate dateOfBirth;

    @Schema(description = "Contact phone number", example = "9876543210")
    private String phone;

    @Schema(description = "Academic department", example = "Computer Science and Engineering")
    private String department;

    @Schema(description = "Academic program / degree", example = "B.Tech CSE")
    private String program;

    @Schema(description = "Current academic year of study", example = "2")
    private Integer yearOfStudy;

    @Schema(description = "Assigned class section", example = "A")
    private String section;

    @Schema(description = "Current student status", example = "ACTIVE")
    private StudentStatus status;

    @Schema(description = "Timestamp when student profile was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when student profile was last updated")
    private LocalDateTime updatedAt;

    public StudentResponse() {
    }

    public StudentResponse(Long id, Long userId, String studentNumber, String firstName, String lastName,
                           LocalDate dateOfBirth, String phone, String department, String program,
                           Integer yearOfStudy, String section, StudentStatus status,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.department = department;
        this.program = program;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StudentResponse fromEntity(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentResponse(
                student.getId(),
                student.getUserId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth(),
                student.getPhone(),
                student.getDepartment(),
                student.getProgram(),
                student.getYearOfStudy(),
                student.getSection(),
                student.getStatus(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
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
