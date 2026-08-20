package com.project.smartparking.repository;

import com.project.smartparking.model.ParkingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByStatus(ParkingSlot.SlotStatus status);

    List<ParkingSlot> findByLot_LotId(Long lotId);

    void deleteByLot_LotId(Long lotId);

    List<ParkingSlot> findByLot_Hub_HubIdAndStatus(Long hubId, ParkingSlot.SlotStatus status);

    @Query(value = "SELECT s.* FROM parking_slots s " +
                   "JOIN parking_lots l ON s.lot_id = l.lot_id " +
                   "WHERE l.hub_id = :hubId", nativeQuery = true)
    List<ParkingSlot> findByHub(@Param("hubId") Long hubId);

   
    @Query(value = "SELECT s.* FROM parking_slots s " +
                   "JOIN parking_lots l ON s.lot_id = l.lot_id " +
                   "WHERE l.hub_id = :hubId AND s.status = 'AVAILABLE'", nativeQuery = true)
    List<ParkingSlot> findAvailableByHub(@Param("hubId") Long hubId);

    @Query(value = "SELECT COUNT(*) FROM parking_slots s " +
                   "JOIN parking_lots l ON s.lot_id = l.lot_id " +
                   "WHERE l.hub_id = :hubId AND s.status = 'AVAILABLE' AND l.status = 'ACTIVE'", nativeQuery = true)
    int countAvailableByHub(@Param("hubId") Long hubId);

    Optional<ParkingSlot> findBySlotCode(String slotCode);

    @Query("SELECT s FROM ParkingSlot s " +
           "JOIN FETCH s.lot l " +
           "JOIN FETCH l.hub h")
    List<ParkingSlot> findAllWithLotAndHub();
    // FIXED: Maps to ParkingSlot -> sensor -> sensorId
    Optional<ParkingSlot> findBySensor_SensorId(String sensorId);
}