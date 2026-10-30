package com.smartcampus.auth.config;

import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import com.smartcampus.auth.repository.RoleRepository;
import com.smartcampus.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotently seeds the initial ADMIN user account on application startup if none exists.
 * The password is read from the configured environment variable and encoded using BCrypt.
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final String firstName;
    private final String lastName;

    public AdminUserInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.init.admin.enabled:true}") boolean enabled,
            @Value("${app.init.admin.username:admin}") String adminUsername,
            @Value("${app.init.admin.password:}") String adminPassword,
            @Value("${app.init.admin.email:admin@smartcampus.com}") String adminEmail,
            @Value("${app.init.admin.first-name:Campus}") String firstName,
            @Value("${app.init.admin.last-name:Admin}") String lastName) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Admin user seeding is disabled (app.init.admin.enabled=false).");
            return;
        }

        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            log.info("INITIAL_ADMIN_PASSWORD is not set or empty. Skipping initial admin user creation.");
            return;
        }

        String trimmedUsername = adminUsername != null ? adminUsername.trim() : "admin";
        String trimmedEmail = adminEmail != null ? adminEmail.trim() : "admin@smartcampus.com";

        if (userRepository.existsByUsername(trimmedUsername)) {
            log.info("Admin user with username '{}' already exists. Skipping initialization.", trimmedUsername);
            return;
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            log.info("Admin user with email '{}' already exists. Skipping initialization.", trimmedEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> {
                    log.warn("Role 'ADMIN' not found in database. Creating 'ADMIN' role.");
                    return roleRepository.save(new Role("ADMIN", "Administrator with full system access"));
                });

        User adminUser = new User();
        adminUser.setUsername(trimmedUsername);
        adminUser.setEmail(trimmedEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(adminPassword.trim()));
        adminUser.setFirstName(firstName != null && !firstName.isBlank() ? firstName.trim() : "Campus");
        adminUser.setLastName(lastName != null && !lastName.isBlank() ? lastName.trim() : "Admin");
        adminUser.setIsActive(true);
        adminUser.addRole(adminRole);

        userRepository.save(adminUser);
        log.info("Successfully initialized admin user '{}' ({}) with role ADMIN.", trimmedUsername, trimmedEmail);

        // Also seed an initial FACULTY account if none exists, ensuring attendance/academic faculty assignments work
        Role facultyRole = roleRepository.findByName("FACULTY")
                .orElseGet(() -> {
                    log.warn("Role 'FACULTY' not found in database. Creating 'FACULTY' role.");
                    return roleRepository.save(new Role("FACULTY", "Faculty member"));
                });

        if (!userRepository.existsByUsername("faculty")) {
            User facultyUser = new User();
            facultyUser.setUsername("faculty");
            facultyUser.setEmail("faculty@smartcampus.com");
            facultyUser.setPasswordHash(passwordEncoder.encode("CampusFaculty@123"));
            facultyUser.setFirstName("Robert");
            facultyUser.setLastName("Smith");
            facultyUser.setIsActive(true);
            facultyUser.addRole(facultyRole);
            userRepository.save(facultyUser);
            log.info("Successfully initialized faculty user 'faculty' (faculty@smartcampus.com) with role FACULTY.");
        }
    }
}

