package com.smartcampus.student.client.impl;

import com.smartcampus.student.client.AuthServiceClient;
import com.smartcampus.student.client.dto.UserValidationResponse;
import com.smartcampus.student.exception.AuthServiceUnavailableException;
import com.smartcampus.student.exception.UserValidationFailedException;
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
    public UserValidationResponse validateUser(Long userId, String requiredRole, String bearerToken) {
        log.info("Initiating Auth Service validation for userId: {} with requiredRole: {}", userId, requiredRole);

        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            log.warn("Missing Authorization Bearer token for Auth Service validation");
            throw new UserValidationFailedException("Authentication token is required for Auth Service validation");
        }

        String authHeader = bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken;

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/internal/users/{userId}/validation")
                            .queryParam("requiredRole", requiredRole)
                            .build(userId))
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        int statusCode = response.getStatusCode().value();
                        if (statusCode == 404) {
                            log.warn("Auth user ID {} not found in auth-service", userId);
                            throw new UserValidationFailedException("Auth user with ID " + userId + " does not exist");
                        } else if (statusCode == 403) {
                            log.warn("Access forbidden during Auth Service validation for userId {}", userId);
                            throw new UserValidationFailedException("Insufficient permissions to validate user with Auth Service");
                        } else if (statusCode == 401) {
                            log.warn("Unauthorized token during Auth Service validation for userId {}", userId);
                            throw new UserValidationFailedException("Invalid or expired authentication token for Auth Service validation");
                        } else {
                            log.warn("Auth validation failed for userId {} with HTTP status {}", userId, statusCode);
                            throw new UserValidationFailedException("User validation failed with status: " + statusCode);
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Auth Service returned 5xx server error for userId {}", userId);
                        throw new AuthServiceUnavailableException("Authentication service is currently experiencing errors");
                    })
                    .body(UserValidationResponse.class);

        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Auth Service at userId {}: {}", userId, ex.getMessage());
            throw new AuthServiceUnavailableException("Authentication service is unavailable. Please try again later.", ex);
        } catch (UserValidationFailedException | AuthServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error validating user with Auth Service: {}", ex.getMessage(), ex);
            throw new AuthServiceUnavailableException("Unexpected error during Auth Service communication", ex);
        }
    }
}
