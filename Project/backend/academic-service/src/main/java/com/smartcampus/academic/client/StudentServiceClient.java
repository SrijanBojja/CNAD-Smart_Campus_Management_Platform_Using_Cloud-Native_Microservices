package com.smartcampus.academic.client;

import com.smartcampus.academic.client.dto.StudentResponseDto;

public interface StudentServiceClient {

    StudentResponseDto getStudentById(Long studentId, String bearerToken);
}
