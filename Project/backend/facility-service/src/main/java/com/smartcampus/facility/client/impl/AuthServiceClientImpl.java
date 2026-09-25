package com.smartcampus.facility.client.impl;

import com.smartcampus.facility.client.AuthServiceClient;
import com.smartcampus.facility.client.dto.UserValidationResponseDto;
import com.smartcampus.facility.exception.BadRequestException;
import com.smartcampus.facility.exception.ServiceUnavailableException;
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
public class AuthServiceClientImpl implements AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClientImpl.class);

    private final RestClient restClient;

    public AuthServiceClientImpl(
            @Value("${auth-service.url:http://localhost:8081}") String authServiceUrl,
            @Value("${auth-service.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${auth-service.read-timeout-ms:5000}") int readTimeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public UserValidationResponseDto validateUser(Long userId, String requiredRole, String bearerToken) {
        log.info("Validating Auth user ID: {} with required role: {}", userId, requiredRole);

        String authHeader = (bearerToken != null && !bearerToken.trim().isEmpty())
                ? (bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken)
                : null;

        try {
            var requestSpec = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/internal/users/{userId}/validation")
                            .queryParam("requiredRole", requiredRole)
                            .build(userId))
                    .accept(MediaType.APPLICATION_JSON);

            if (authHeader != null) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }

            return requestSpec.retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        if (statusCode == 404) {
                            log.warn("Auth user ID {} not found", userId);
                            throw new BadRequestException("User with ID " + userId + " does not exist");
                        } else if (statusCode == 403 || statusCode == 401) {
                            log.warn("Unauthorized/Forbidden while validating Auth user {}", userId);
                            throw new BadRequestException("Authorization failure validating user ID " + userId);
                        } else {
                            log.warn("Auth Service returned status {} for user {}", statusCode, userId);
                            throw new BadRequestException("User validation failed with status: " + statusCode);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Auth Service returned 5xx for userId {}", userId);
                        throw new ServiceUnavailableException("Authentication service is currently experiencing errors");
                    })
                    .body(UserValidationResponseDto.class);

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Auth Service for userId {}: {}", userId, ex.getMessage());
            throw new ServiceUnavailableException("Authentication service is unavailable. Please try again later.", ex);
        } catch (BadRequestException | ServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error contacting Auth Service for userId {}: {}", userId, ex.getMessage(), ex);
            throw new ServiceUnavailableException("Unexpected error during Auth Service communication", ex);
        }
    }
}
