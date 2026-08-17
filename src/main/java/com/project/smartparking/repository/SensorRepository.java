package com.project.smartparking.repository;

import com.project.smartparking.dto.SensorDTO;
import com.project.smartparking.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

    @Query("SELECT new com.project.smartparking.dto.SensorDTO(" +
           "s.sensorId, s.slotId, s.sensorType, s.macAddress, s.lastValue, " +
           "s.status, s.createdAt, ps.slotCode, s.gpioPin, s.zoneCode) " +
           "FROM Sensor s LEFT JOIN ParkingSlot ps ON s.slotId = ps.slotId")
    List<SensorDTO> findAllWithSlotCode();

    @Query("SELECT new com.project.smartparking.dto.SensorDTO(" +
           "s.sensorId, s.slotId, s.sensorType, s.macAddress, s.lastValue, " +
           "s.status, s.createdAt, ps.slotCode, s.gpioPin, s.zoneCode) " +
           "FROM Sensor s LEFT JOIN ParkingSlot ps ON s.slotId = ps.slotId " +
           "WHERE s.macAddress = :macAddress")
    List<SensorDTO> findByMacAddress(@Param("macAddress") String macAddress);
}