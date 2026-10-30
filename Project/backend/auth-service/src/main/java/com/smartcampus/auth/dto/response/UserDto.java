package com.smartcampus.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "User profile information")
public class UserDto {

    @Schema(description = "Unique user identifier", example = "1")
    private Long id;

    @Schema(description = "Unique username", example = "srijan")
    private String username;

    @Schema(description = "User email address", example = "srijan@example.com")
    private String email;

    @Schema(description = "User first name", example = "Srijan")
    private String firstName;

    @Schema(description = "User last name", example = "Bojja")
    private String lastName;

    @Schema(description = "Assigned user roles", example = "[\"STUDENT\"]")
    private List<String> roles;

    @Schema(description = "Employee ID", example = "FAC-1001")
    private String employeeId;

    @Schema(description = "Academic department", example = "Computer Science")
    private String department;

    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    public UserDto() {
    }

    public UserDto(Long id, String username, String email, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public UserDto(Long id, String username, String email, String firstName, String lastName, List<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }

    public UserDto(Long id, String username, String email, String firstName, String lastName, List<String> roles,
                   String employeeId, String department, String phone) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.employeeId = employeeId;
        this.department = department;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoneNumber() {
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDto userDto = (UserDto) o;
        return Objects.equals(id, userDto.id) && Objects.equals(username, userDto.username) && Objects.equals(email, userDto.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }
}
