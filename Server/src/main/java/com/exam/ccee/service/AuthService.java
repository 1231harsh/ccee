package com.exam.ccee.service;

import com.exam.ccee.entity.User;
import com.exam.ccee.exception.ApiException;
import com.exam.ccee.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {


    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;


    AuthService(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public String register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);

        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "That username already exists. Please choose a different username.");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(encoder.encode(password));

        userRepository.save(user);

        return "Registered successfully";
    }

    public User login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password."));

        if (!encoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

        return user;
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username is required.");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        String value = password == null ? "" : password;
        if (value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required.");
        }
        if (value.length() < 4) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 4 characters long.");
        }
    }
}
