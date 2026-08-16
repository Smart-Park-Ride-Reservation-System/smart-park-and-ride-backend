package com.project.smartparking.repository;

import com.project.smartparking.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByMacAddress(String macAddress);
}