package com.project.smartparking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.project.smartparking.service.JwtService;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.AuthRepository;

import java.util.Optional;

import com.project.smartparking.dto.SignupRequest;

@Service
public class AuthService {

    @Autowired
    private AuthRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // ✅ returns full User so controller can extract role + firstName
    public User loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || password == null) return null;

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) return null;

        return user;
    }

    public User signup(SignupRequest request) {
        if (request.getPassword() == null || request.getEmail() == null) {
            return null;
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return null;
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRole(User.Role.COMMUTER);

        return userRepository.save(newUser);
    }
}