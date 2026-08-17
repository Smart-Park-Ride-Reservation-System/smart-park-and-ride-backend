package com.project.smartparking.controller;

import com.project.smartparking.dto.SensorConfigDTO;
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

        Set<Long> slotIds = sensors.stream()
                .map(Sensor::getSlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> slotMap = slotRepo.findAllById(slotIds).stream()
                .collect(Collectors.toMap(ParkingSlot::getSlotId, ParkingSlot::getSlotCode));

        // SensorController.java (lines 40-52)
        return sensors.stream()
                .map(s -> new SensorDTO(
                        s.getSensorId(),
                        s.getSlotId(),
                        s.getSensorType(),
                        s.getMacAddress(),
                        s.getLastValue(),
                        s.getStatus(),
                        s.getCreatedAt(),
                        s.getSlotId() != null ? slotMap.get(s.getSlotId()) : null,
                        null, 
                        null 
                ))
                .toList();
    }

    @GetMapping("/config")
    public List<SensorConfigDTO> getConfigByMac(@RequestParam("mac") String mac) {
        List<Sensor> sensors = sensorRepo.findAll().stream()
                .filter(s -> mac.equalsIgnoreCase(s.getMacAddress()))
                .filter(s -> !"FAULTY".equalsIgnoreCase(s.getStatus()))
                .toList();

        if (sensors.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> slotIds = sensors.stream()
                .map(Sensor::getSlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> slotMap = slotRepo.findAllById(slotIds).stream()
                .collect(Collectors.toMap(ParkingSlot::getSlotId, ParkingSlot::getSlotCode));

        return sensors.stream()
                .map(s -> {
                    String slotCode = slotMap.getOrDefault(s.getSlotId(), "A-1");

                    // Parse Zone ("A-01" -> "A") and Slot ("A-01" -> "1")
                    String zone = slotCode.contains("-") ? slotCode.split("-")[0] : "A";
                    String slotNum = slotCode.contains("-")
                            ? slotCode.split("-")[1].replaceFirst("^0+", "")
                            : slotCode;

                    // Extract GPIO number from sensorType (e.g. "GPIO_34" -> 34)
                    int pin = 34; // Fallback
                    if (s.getSensorType() != null && s.getSensorType().startsWith("GPIO_")) {
                        try {
                            pin = Integer.parseInt(s.getSensorType().replace("GPIO_", ""));
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    return new SensorConfigDTO(zone, slotNum, pin);
                })
                .toList();
    }
}