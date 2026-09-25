package com.smartcampus.student.entity;

public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    GRADUATED;

    public static StudentStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StudentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
