package com.project.smartparking.repository;

import com.project.smartparking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}