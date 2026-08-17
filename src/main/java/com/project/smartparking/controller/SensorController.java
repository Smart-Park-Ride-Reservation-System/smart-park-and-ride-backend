package com.project.smartparking.controller;

import com.project.smartparking.dto.SensorDTO;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Sensor;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.SensorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorRepository sensorRepo;
    private final ParkingSlotRepository slotRepo;

    public SensorController(SensorRepository sensorRepo, ParkingSlotRepository slotRepo) {
        this.sensorRepo = sensorRepo;
        this.slotRepo = slotRepo;
    }

    @GetMapping
    public List<SensorDTO> getAll() {
        List<Sensor> sensors = sensorRepo.findAll();
        if (sensors.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect all non-null slotIds to query in a single batch SQL query (fixes N+1 problem)
        Set<Long> slotIds = sensors.stream()
                .map(Sensor::getSlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> slotMap = slotRepo.findAllById(slotIds).stream()
                .collect(Collectors.toMap(ParkingSlot::getSlotId, ParkingSlot::getSlotCode));

        return sensors.stream()
                .map(s -> new SensorDTO(
                        s.getSensorId(),
                        s.getSlotId(),
                        s.getSensorType(),
                        s.getMacAddress(),
                        s.getLastValue(),
                        s.getStatus(),
                        s.getCreatedAt(),
                        s.getSlotId() != null ? slotMap.get(s.getSlotId()) : null
                ))
                .toList();
    }
}