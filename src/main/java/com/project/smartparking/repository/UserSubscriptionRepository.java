package com.project.smartparking.repository;

import com.project.smartparking.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    // --- Search by User ID ---
    List<UserSubscription> findByUser_UserId(Long userId);

    List<UserSubscription> findByUser_UserIdAndStatus(Long userId, String status);

    Optional<UserSubscription> findFirstByUser_UserIdAndStatusOrderByEndDateDesc(Long userId, String status);

    // --- Search by User Email ---
    List<UserSubscription> findByUser_Email(String email);

    List<UserSubscription> findByUser_EmailAndStatus(String email, String status);

    Optional<UserSubscription> findFirstByUser_EmailAndStatusOrderByEndDateDesc(String email, String status);

    // --- Custom JPQL Queries ---
    @Query("SELECT us FROM UserSubscription us " +
           "WHERE us.status = 'ACTIVE' AND us.endDate > :now")
    List<UserSubscription> findCurrentlyActive(@Param("now") Timestamp now);

    @Query("SELECT s FROM UserSubscription s " +
           "JOIN FETCH s.user " +
           "JOIN FETCH s.subscriptionPackage " +
           "ORDER BY s.startDate DESC")
    List<UserSubscription> findAllWithUserAndPackage();
}