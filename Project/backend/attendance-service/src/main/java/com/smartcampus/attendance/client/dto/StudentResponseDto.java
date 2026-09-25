package com.smartcampus.attendance.client.dto;

public class StudentResponseDto {
    private Long id;
    private Long userId;
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String status;

    public StudentResponseDto() {
    }

    public StudentResponseDto(Long id, Long userId, String studentNumber, String firstName, String lastName, String status) {
        this.id = id;
        this.userId = userId;
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
