package com.project.smartparking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.AuthRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import com.project.smartparking.dto.SignupRequest;

@Service
public class AuthService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public User loginUser(String email, String password) {
        Optional<User> userOpt = authRepository.findByEmail(email);
        if (userOpt.isEmpty() || password == null) return null;

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) return null;

        return user;
    }

    public User signup(SignupRequest request) {
        if (request.getPassword() == null || request.getEmail() == null) {
            return null;
        }

        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            return null;
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPhone(request.getPhone());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(User.Role.COMMUTER);

        return authRepository.save(newUser);
    }

    public User findUserByEmail(String email) {
        return authRepository.findByEmail(email).orElse(null);
    }

    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = authRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Silently no-op so we don't reveal whether the email exists
            return;
        }

        User user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();

        user.setResetToken(resetToken);
        user.setResetTokenExpiry(Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        authRepository.save(user);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset your SmartParking password";
        String body = "We received a request to reset your password.\n\n"
                + "Click the link below to choose a new password. This link expires in 1 hour.\n\n"
                + resetLink + "\n\n"
                + "If you didn't request this, you can safely ignore this email.";

        emailService.send(user.getEmail(), subject, body);
    }

    /**
     * Returns true if the password was reset successfully, false if the token
     * is invalid or expired.
     */
    public boolean resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }

        Optional<User> userOpt = authRepository.findByResetToken(token);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().toInstant().isBefore(Instant.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        authRepository.save(user);

        return true;
    }
}
