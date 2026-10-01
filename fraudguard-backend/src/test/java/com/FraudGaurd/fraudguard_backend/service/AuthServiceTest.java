package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidCredentialsException;
import com.FraudGaurd.fraudguard_backend.ExceptionHandler.UsernameAlreadyExistsException;
import com.FraudGaurd.fraudguard_backend.model.User;
import com.FraudGaurd.fraudguard_backend.model.UserRole;
import com.FraudGaurd.fraudguard_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void registerShouldHashPasswordAndAssignUserRole() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register("testuser", "TestPassword123");

        assertEquals("testuser", registered.getUsername());
        assertEquals(UserRole.USER, registered.getRole());
        assertNotEquals("TestPassword123", registered.getPassword());
        assertTrue(passwordEncoder.matches("TestPassword123", registered.getPassword()));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectExistingUsername() {
        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(new User()));

        assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authService.register("testuser", "TestPassword123")
        );
    }

    @Test
    void authenticateShouldReturnUserForCorrectPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("TestPassword123"));
        user.setRole(UserRole.USER);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        assertEquals(
                user,
                authService.authenticate("testuser", "TestPassword123")
        );
    }

    @Test
    void authenticateShouldRejectIncorrectPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("TestPassword123"));

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate("testuser", "wrong-password")
        );
    }

    @Test
    void authenticateShouldRejectUnknownUsername() {
        when(userRepository.findByUsername("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.authenticate("missing", "TestPassword123")
        );
    }
}
