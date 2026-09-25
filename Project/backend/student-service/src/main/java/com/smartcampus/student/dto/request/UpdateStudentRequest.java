package com.smartcampus.student.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Schema(description = "Request payload for updating student profile information")
public class UpdateStudentRequest {

    @Schema(description = "Student first name", example = "Srijan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must not exceed 100 characters")
    private String firstName;

    @Schema(description = "Student last name", example = "Bojja", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must not exceed 100 characters")
    private String lastName;

    @Schema(description = "Date of birth (YYYY-MM-DD)", example = "2005-08-15")
    @Past(message = "dateOfBirth must be in the past")
    private LocalDate dateOfBirth;

    @Schema(description = "Contact phone number", example = "9876543210")
    @Size(max = 30, message = "phone must not exceed 30 characters")
    private String phone;

    @Schema(description = "Academic department", example = "Computer Science and Engineering")
    @Size(max = 100, message = "department must not exceed 100 characters")
    private String department;

    @Schema(description = "Academic program / degree", example = "B.Tech CSE")
    @Size(max = 100, message = "program must not exceed 100 characters")
    private String program;

    @Schema(description = "Current academic year of study", example = "3")
    @Min(value = 1, message = "yearOfStudy must be at least 1")
    @Max(value = 10, message = "yearOfStudy must not exceed 10")
    private Integer yearOfStudy;

    @Schema(description = "Assigned class section", example = "B")
    @Size(max = 50, message = "section must not exceed 50 characters")
    private String section;

    public UpdateStudentRequest() {
    }

    public UpdateStudentRequest(String firstName, String lastName, LocalDate dateOfBirth,
                                String phone, String department, String program,
                                Integer yearOfStudy, String section) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.department = department;
        this.program = program;
        this.yearOfStudy = yearOfStudy;
        this.section = section;
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
}
