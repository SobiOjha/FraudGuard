package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidCredentialsException;
import com.FraudGaurd.fraudguard_backend.ExceptionHandler.UsernameAlreadyExistsException;
import com.FraudGaurd.fraudguard_backend.model.User;
import com.FraudGaurd.fraudguard_backend.model.UserRole;
import com.FraudGaurd.fraudguard_backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }

        User user = new User();

        user.setUsername(username);

        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        user.setRole(UserRole.USER);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            // The database unique constraint also protects against concurrent registrations.
            throw new UsernameAlreadyExistsException();
        }
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
