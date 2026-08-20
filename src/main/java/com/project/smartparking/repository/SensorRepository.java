package com.project.smartparking.repository;

import com.project.smartparking.dto.SensorDTO;
import com.project.smartparking.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SensorRepository extends JpaRepository<Sensor, String> {

    @Query("SELECT new com.project.smartparking.dto.SensorDTO(" +
           "s.sensorId, " +
           "s.slot.slotId, " +
           "s.sensorType, " +
           "s.macAddress, " +
           "s.lastValue, " +
           "s.status, " +
           "s.createdAt, " +
           "ps.slotCode, " +
           "s.gpioPin, " +
           "s.lot.lotId, " +
           "s.lot.lotCode, " +
           "s.lot.name) " +
           "FROM Sensor s LEFT JOIN s.slot ps LEFT JOIN s.lot")
    List<SensorDTO> findAllWithSlotCode();

    @Query("SELECT new com.project.smartparking.dto.SensorDTO(" +
           "s.sensorId, " +
           "s.slot.slotId, " +
           "s.sensorType, " +
           "s.macAddress, " +
           "s.lastValue, " +
           "s.status, " +
           "s.createdAt, " +
           "ps.slotCode, " +
           "s.gpioPin, " +
           "s.lot.lotId, " +
           "s.lot.lotCode, " +
           "s.lot.name) " +
           "FROM Sensor s LEFT JOIN s.slot ps LEFT JOIN s.lot " +
           "WHERE s.macAddress = :macAddress")
    List<SensorDTO> findByMacAddress(@Param("macAddress") String macAddress);
}