package com.smartcampus.academic.client.impl;

import com.smartcampus.academic.client.StudentServiceClient;
import com.smartcampus.academic.client.dto.StudentResponseDto;
import com.smartcampus.academic.exception.BadRequestException;
import com.smartcampus.academic.exception.ResourceNotFoundException;
import com.smartcampus.academic.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class StudentServiceClientImpl implements StudentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceClientImpl.class);

    private final RestClient restClient;

    public StudentServiceClientImpl(
            @Value("${student-service.url:http://localhost:8082}") String studentServiceUrl,
            @Value("${student-service.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${student-service.read-timeout-ms:5000}") int readTimeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(studentServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public StudentResponseDto getStudentById(Long studentId, String bearerToken) {
        log.info("Fetching student details from Student Service for studentId: {}", studentId);

        String authHeader = (bearerToken != null && !bearerToken.trim().isEmpty())
                ? (bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken)
                : null;

        try {
            var requestSpec = restClient.get()
                    .uri("/api/v1/students/{id}", studentId)
                    .accept(MediaType.APPLICATION_JSON);

            if (authHeader != null) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            return requestSpec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        if (statusCode == 404) {
                            log.warn("Student with ID {} not found in Student Service", studentId);
                            throw new ResourceNotFoundException("Referenced Student with ID " + studentId + " does not exist");
                        } else if (statusCode == 403 || statusCode == 401) {
                            log.warn("Access denied/unauthorized while calling Student Service for studentId {}", studentId);
                            throw new BadRequestException("Permission or authorization failure validating student ID " + studentId);
                        } else {
                            log.warn("Student Service returned error {} for studentId {}", statusCode, studentId);
                            throw new BadRequestException("Validation failure with Student Service for ID " + studentId);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Student Service 5xx error for studentId {}", studentId);
                        throw new ServiceUnavailableException("Student service is currently experiencing errors");
                    })
                    .body(StudentResponseDto.class);

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Student Service for studentId {}: {}", studentId, ex.getMessage());
            throw new ServiceUnavailableException("Student service is unavailable. Please try again later.", ex);
        } catch (ResourceNotFoundException | BadRequestException | ServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error contacting Student Service for studentId {}: {}", studentId, ex.getMessage(), ex);
            throw new ServiceUnavailableException("Unexpected error during Student Service communication", ex);
        }
    }
}
