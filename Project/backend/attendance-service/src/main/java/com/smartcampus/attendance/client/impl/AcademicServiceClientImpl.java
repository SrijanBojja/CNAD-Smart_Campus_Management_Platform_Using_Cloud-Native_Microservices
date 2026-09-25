package com.smartcampus.attendance.client.impl;

import com.smartcampus.attendance.client.AcademicServiceClient;
import com.smartcampus.attendance.client.dto.SubjectResponseDto;
import com.smartcampus.attendance.exception.BadRequestException;
import com.smartcampus.attendance.exception.ResourceNotFoundException;
import com.smartcampus.attendance.exception.ServiceUnavailableException;
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
public class AcademicServiceClientImpl implements AcademicServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AcademicServiceClientImpl.class);

    private final RestClient restClient;

    public AcademicServiceClientImpl(
            @Value("${academic-service.url:http://localhost:8083}") String academicServiceUrl,
            @Value("${academic-service.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${academic-service.read-timeout-ms:5000}") int readTimeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(academicServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public SubjectResponseDto getSubjectById(Long subjectId, String bearerToken) {
        log.info("Fetching subject details from Academic Service for subjectId: {}", subjectId);

        String authHeader = (bearerToken != null && !bearerToken.trim().isEmpty())
                ? (bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken)
                : null;

        try {
            var requestSpec = restClient.get()
                    .uri("/api/v1/subjects/{id}", subjectId)
                    .accept(MediaType.APPLICATION_JSON);

            if (authHeader != null) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            return requestSpec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        if (statusCode == 404) {
                            log.warn("Subject with ID {} not found in Academic Service", subjectId);
                            throw new ResourceNotFoundException("Referenced Subject with ID " + subjectId + " does not exist");
                        } else if (statusCode == 403 || statusCode == 401) {
                            log.warn("Access denied/unauthorized while calling Academic Service for subjectId {}", subjectId);
                            throw new BadRequestException("Permission or authorization failure validating subject ID " + subjectId);
                        } else {
                            log.warn("Academic Service returned error {} for subjectId {}", statusCode, subjectId);
                            throw new BadRequestException("Validation failure with Academic Service for ID " + subjectId);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Academic Service 5xx error for subjectId {}", subjectId);
                        throw new ServiceUnavailableException("Academic service is currently experiencing errors");
                    })
                    .body(SubjectResponseDto.class);

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Academic Service for subjectId {}: {}", subjectId, ex.getMessage());
            throw new ServiceUnavailableException("Academic service is unavailable. Please try again later.", ex);
        } catch (ResourceNotFoundException | BadRequestException | ServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error contacting Academic Service for subjectId {}: {}", subjectId, ex.getMessage(), ex);
            throw new ServiceUnavailableException("Unexpected error during Academic Service communication", ex);
        }
    }
}
