package com.smartcampus.auth.service.impl;

import com.smartcampus.auth.dto.request.LoginRequest;
import com.smartcampus.auth.dto.request.RegisterFacultyRequest;
import com.smartcampus.auth.dto.request.RegisterStudentRequest;
import com.smartcampus.auth.dto.response.AuthResponse;
import com.smartcampus.auth.dto.response.UserDto;
import com.smartcampus.auth.dto.response.UserValidationResponse;
import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.exception.BadRequestException;
import com.smartcampus.auth.exception.DuplicateResourceException;
import com.smartcampus.auth.exception.InvalidCredentialsException;
import com.smartcampus.auth.exception.ResourceNotFoundException;
import com.smartcampus.auth.exception.UserDisabledException;
import com.smartcampus.auth.repository.RoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "FACULTY", "STUDENT");

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           JwtTokenProvider tokenProvider,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

            UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getFirstName(), user.getLastName(), roles);

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

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                user.getEmployeeId(),
                user.getDepartment(),
                user.getPhone()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserValidationResponse validateUser(Long userId, String requiredRole) {
        log.info("Validating user ID: {} with requiredRole: {}", userId, requiredRole);

        String targetRole = null;
        if (requiredRole != null && !requiredRole.trim().isEmpty()) {
            String normalizedRole = requiredRole.trim().toUpperCase();
            if (normalizedRole.startsWith("ROLE_")) {
                normalizedRole = normalizedRole.substring(5);
            }

            if (!VALID_ROLES.contains(normalizedRole)) {
                throw new BadRequestException("Invalid requiredRole '" + requiredRole + "'. Valid roles are: " + VALID_ROLES);
            }
            targetRole = normalizedRole;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        boolean active = Boolean.TRUE.equals(user.getIsActive());

        if (targetRole == null) {
            log.info("Validation completed for user ID {}: active={}, no specific role required", userId, active);
            return new UserValidationResponse(user.getId(), active, true);
        }

        final String finalTargetRole = targetRole;
        boolean hasRequiredRole = user.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .anyMatch(name -> name.equalsIgnoreCase(finalTargetRole));

        log.info("Validation completed for user ID {}: active={}, hasRole {}={}", userId, active, finalTargetRole, hasRequiredRole);
        return new UserValidationResponse(user.getId(), active, hasRequiredRole);
    }

    @Override
    @Transactional
    public UserDto registerStudent(RegisterStudentRequest request) {
        log.info("Processing student registration request for username: '{}', email: '{}'",
                request.getUsername(), request.getEmail());

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            log.warn("Student registration failed: username '{}' already exists", username);
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Student registration failed: email '{}' already registered", email);
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }

        Role studentRole = (roleRepository != null)
                ? roleRepository.findByName("STUDENT").orElseGet(() -> {
                    log.warn("Role 'STUDENT' not found in database. Initializing default STUDENT role.");
                    return roleRepository.save(new Role("STUDENT", "Student with campus learning and portal access"));
                })
                : new Role(3L, "STUDENT", "Student with campus learning and portal access");

        String passwordHash = (passwordEncoder != null)
                ? passwordEncoder.encode(request.getPassword())
                : request.getPassword();

        String firstName = (request.getFirstName() != null && !request.getFirstName().trim().isEmpty())
                ? request.getFirstName().trim()
                : username;
        String lastName = (request.getLastName() != null && !request.getLastName().trim().isEmpty())
                ? request.getLastName().trim()
                : "Student";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive(true);
        user.addRole(studentRole);

        User savedUser = userRepository.save(user);

        log.info("Student user account registered successfully with ID: {}, username: '{}'",
                savedUser.getId(), savedUser.getUsername());

        List<String> roles = savedUser.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .collect(Collectors.toList());

        return new UserDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                roles
        );
    }

    @Override
    @Transactional
    public UserDto registerFaculty(RegisterFacultyRequest request) {
        log.info("Processing faculty registration request for username: '{}', email: '{}'",
                request.getUsername(), request.getEmail());

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            log.warn("Faculty registration failed: username '{}' already exists", username);
            throw new DuplicateResourceException("Username '" + username + "' is already taken");
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Faculty registration failed: email '{}' already registered", email);
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }

        Role facultyRole = (roleRepository != null)
                ? roleRepository.findByName("FACULTY").orElseGet(() -> {
                    log.warn("Role 'FACULTY' not found in database. Initializing default FACULTY role.");
                    return roleRepository.save(new Role("FACULTY", "Faculty member with academic access"));
                })
                : new Role(2L, "FACULTY", "Faculty member with academic access");

        String passwordHash = (passwordEncoder != null)
                ? passwordEncoder.encode(request.getPassword())
                : request.getPassword();

        String firstName = request.getFirstName() != null ? request.getFirstName().trim() : username;
        String lastName = request.getLastName() != null ? request.getLastName().trim() : "Faculty";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmployeeId(request.getEmployeeId() != null && !request.getEmployeeId().isBlank() ? request.getEmployeeId().trim() : null);
        user.setDepartment(request.getDepartment() != null && !request.getDepartment().isBlank() ? request.getDepartment().trim() : null);
        String phone = request.getPhoneNumber() != null ? request.getPhoneNumber() : request.getPhone();
        user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        user.setIsActive(true);
        user.addRole(facultyRole);

        User savedUser = userRepository.save(user);

        log.info("Faculty user account registered successfully with ID: {}, username: '{}'",
                savedUser.getId(), savedUser.getUsername());

        List<String> roles = savedUser.getRoles().stream()
                .map(Role::getName)
                .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                .collect(Collectors.toList());

        return new UserDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                roles,
                savedUser.getEmployeeId(),
                savedUser.getDepartment(),
                savedUser.getPhone()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getFacultyUsers() {
        return userRepository.findByRoles_Name("FACULTY").stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(user -> {
                    List<String> roles = user.getRoles().stream()
                            .map(Role::getName)
                            .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
                            .collect(Collectors.toList());
                    return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                user.getEmployeeId(),
                user.getDepartment(),
                user.getPhone()
        );
                })
                .collect(Collectors.toList());
    }
}
