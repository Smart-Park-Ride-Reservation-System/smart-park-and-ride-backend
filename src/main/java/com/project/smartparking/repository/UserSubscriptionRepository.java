package com.project.smartparking.repository;

import com.project.smartparking.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUser_UserId(Long userId);
    List<UserSubscription> findByUser_UserIdAndStatus(Long userId, String status);
    
    // Get the active subscription if one exists (assumes one active at a time)
    Optional<UserSubscription> findFirstByUser_UserIdAndStatusOrderByEndDateDesc(Long userId, String status);
}
