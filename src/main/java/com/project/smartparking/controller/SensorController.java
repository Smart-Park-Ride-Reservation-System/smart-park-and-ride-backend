package com.project.smartparking.controller;

import com.project.smartparking.dto.SensorDTO;
import com.project.smartparking.model.Sensor;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.SensorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return sensorRepo.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private SensorDTO toDTO(Sensor s) {
        String slotCode = s.getSlotId() != null
                ? slotRepo.findById(s.getSlotId()).map(slot -> slot.getSlotCode()).orElse(null)
                : null;

        return new SensorDTO(
                s.getSensorId(), s.getSlotId(), s.getSensorType(), s.getMacAddress(),
                s.getLastValue(), s.getStatus(), s.getCreatedAt(), slotCode
        );
    }
}