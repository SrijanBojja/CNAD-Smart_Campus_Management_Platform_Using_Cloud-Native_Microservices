package com.smartcampus.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a faculty authentication account (ADMIN only)")
public class RegisterFacultyRequest {

    @Schema(description = "Unique username for faculty authentication", example = "prof_smith", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Unique faculty email address", example = "smith@smartcampus.edu", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @Schema(description = "Initial password for faculty login", example = "FacultyPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "Faculty first name", example = "Robert", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Schema(description = "Faculty last name", example = "Smith", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Schema(description = "Employee ID number", example = "FAC-2024-001")
    @Size(max = 50, message = "Employee ID cannot exceed 50 characters")
    private String employeeId;

    @Schema(description = "Academic department", example = "Computer Science")
    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Schema(description = "Contact phone number", example = "+1-555-0199")
    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String phoneNumber;

    public RegisterFacultyRequest() {
    }

    public RegisterFacultyRequest(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhone() {
        return phoneNumber;
    }

    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }
}

