package com.smartcampus.attendance.client;

import com.smartcampus.attendance.client.dto.SubjectResponseDto;

public interface AcademicServiceClient {
    SubjectResponseDto getSubjectById(Long subjectId, String bearerToken);
}
