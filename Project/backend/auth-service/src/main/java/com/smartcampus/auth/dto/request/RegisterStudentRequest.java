package com.smartcampus.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for registering a student authentication account (ADMIN only)")
public class RegisterStudentRequest {

    @Schema(description = "Unique username for student authentication", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Unique student email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @Schema(description = "Initial password for student login", example = "StudentPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "Student first name", example = "John")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Schema(description = "Student last name", example = "Doe")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    public RegisterStudentRequest() {
    }

    public RegisterStudentRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public RegisterStudentRequest(String username, String email, String password, String firstName, String lastName) {
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
}
