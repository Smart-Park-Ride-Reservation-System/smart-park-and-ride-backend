package com.project.smartparking.repository;

import com.project.smartparking.model.UserSubscription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUser_UserId(Long userId);

    List<UserSubscription> findByUser_UserIdAndStatus(Long userId, String status);

    Optional<UserSubscription> findFirstByUser_UserIdAndStatusOrderByEndDateDesc(Long userId, String status);

    @Query("SELECT us FROM UserSubscription us " +
            "WHERE us.status = 'ACTIVE' AND us.endDate > :now")
    List<UserSubscription> findCurrentlyActive(@Param("now") Timestamp now);
}