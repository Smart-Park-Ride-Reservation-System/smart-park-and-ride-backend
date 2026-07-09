package com.project.smartparking.repository;

import com.project.smartparking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.hub.hubId = :hubId")
    List<User> findByHubId(@Param("hubId") Long hubId);
}