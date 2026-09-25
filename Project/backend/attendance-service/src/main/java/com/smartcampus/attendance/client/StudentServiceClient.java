package com.smartcampus.attendance.client;

import com.smartcampus.attendance.client.dto.StudentResponseDto;

public interface StudentServiceClient {
    StudentResponseDto getStudentById(Long studentId, String bearerToken);
}
