package com.smartcampus.student.exception;

public class UserValidationFailedException extends RuntimeException {
    public UserValidationFailedException(String message) {
        super(message);
    }
}
