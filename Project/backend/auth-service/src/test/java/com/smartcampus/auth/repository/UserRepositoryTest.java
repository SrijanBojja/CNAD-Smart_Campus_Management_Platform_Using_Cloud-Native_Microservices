package com.smartcampus.auth.repository;

import com.smartcampus.auth.entity.Role;
import com.smartcampus.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Should persist and find user with roles")
    void testSaveAndFindUser() {
        Role adminRole = new Role("ADMIN", "Admin Role");
        entityManager.persist(adminRole);

        User user = new User("admin_test", "admin_test@example.com", "hash123", "Admin", "User");
        user.addRole(adminRole);
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> found = userRepository.findByUsername("admin_test");
        assertTrue(found.isPresent());
        assertEquals("admin_test@example.com", found.get().getEmail());
        assertEquals(1, found.get().getRoles().size());
        assertTrue(found.get().getRoles().contains(adminRole));
    }

    @Test
    @DisplayName("Should find user by username or email")
    void testFindByUsernameOrEmail() {
        User user = new User("faculty1", "faculty1@example.com", "hash456", "Faculty", "One");
        entityManager.persist(user);
        entityManager.flush();

        Optional<User> byUsername = userRepository.findByUsernameOrEmail("faculty1", "faculty1");
        assertTrue(byUsername.isPresent());

        Optional<User> byEmail = userRepository.findByUsernameOrEmail("faculty1@example.com", "faculty1@example.com");
        assertTrue(byEmail.isPresent());
    }
}
