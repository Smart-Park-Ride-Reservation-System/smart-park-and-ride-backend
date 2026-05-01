package com.project.smartparking.repository;

import com.project.smartparking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // User views their reservations
    List<Reservation> findByUserId(Long userId);
    
    // Admin checks if slot has active reservation
    List<Reservation> findBySlotId(Long slotId);
    
    // Admin filters by status
    List<Reservation> findByStatus(String status);
}