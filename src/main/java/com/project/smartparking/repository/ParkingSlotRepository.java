package com.project.smartparking.repository;

import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByStatus(ParkingSlot.SlotStatus status);

    @Query(value = "SELECT s.* FROM parking_slots s " +
           "JOIN parking_lots l ON s.lot_id = l.lot_id " +
           "WHERE l.hub_id = :hubId AND s.status = 'AVAILABLE'", 
           nativeQuery = true)
    List<ParkingSlot> findAvailableByHub(@Param("hubId") Long hubId);
}