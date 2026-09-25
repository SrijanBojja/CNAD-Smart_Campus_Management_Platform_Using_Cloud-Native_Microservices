package com.smartcampus.auth.service.impl;

import com.smartcampus.auth.dto.request.LoginRequest;
import com.smartcampus.auth.dto.response.AuthResponse;
import com.smartcampus.auth.dto.response.UserDto;
import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.exception.InvalidCredentialsException;
import com.smartcampus.auth.exception.ResourceNotFoundException;
import com.smartcampus.auth.exception.UserDisabledException;
import com.smartcampus.auth.repository.UserRepository;
import com.smartcampus.auth.security.JwtTokenProvider;
import com.smartcampus.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting authentication for username: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUsername()));

            if (Boolean.FALSE.equals(user.getIsActive())) {
                log.warn("Authentication rejected: Account is inactive for user '{}'", user.getUsername());
                throw new UserDisabledException("User account is disabled. Please contact campus administration.");
            }

            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                    .collect(Collectors.toList());

            String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), roles);
            long expiresIn = tokenProvider.getExpirationMs();

            UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail(), roles);

            log.info("Authentication successful for user '{}' with roles {}", user.getUsername(), roles);
            return new AuthResponse(token, "Bearer", expiresIn, userDto);

        } catch (BadCredentialsException ex) {
            log.warn("Authentication failed: invalid credentials for username '{}'", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        } catch (DisabledException ex) {
            log.warn("Authentication failed: account is disabled for username '{}'", request.getUsername());
            throw new UserDisabledException("User account is disabled");
        } catch (AuthenticationException ex) {
            log.warn("Authentication error for username '{}': {}", request.getUsername(), ex.getMessage());
            throw new InvalidCredentialsException("Authentication failed: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UserDisabledException("User account is disabled");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .collect(Collectors.toList());

        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), roles);
    }
}
