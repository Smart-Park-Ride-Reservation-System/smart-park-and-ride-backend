package com.project.smartparking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.smartparking.model.User;
import com.project.smartparking.repository.UserRepository;

import java.util.Optional;

import com.project.smartparking.dto.SignupRequest;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public boolean login(String email, String password) {

    Optional<User> user = userRepository.findByEmail(email);

    if (user.isEmpty() || password == null) {
        return false;
    }

    String dbPassword = user.get().getPassword();

    return dbPassword != null && dbPassword.equals(password);
}

    public User signup(SignupRequest request) {

        if (request.getPassword() == null || request.getPassword() == null) {
            return null;  // Invalid input
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return null;  // Email already exists
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());

        return userRepository.save(newUser);
    }

}
