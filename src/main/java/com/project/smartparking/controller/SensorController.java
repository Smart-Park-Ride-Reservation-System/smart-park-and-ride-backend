package com.project.smartparking.controller;

import com.project.smartparking.dto.SensorConfigDTO;
import com.project.smartparking.dto.SensorDTO;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.model.Sensor;
import com.project.smartparking.repository.ParkingLotRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.repository.SensorRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorRepository sensorRepo;
    private final ParkingSlotRepository slotRepo;
    private final ParkingLotRepository lotRepo;

    public SensorController(SensorRepository sensorRepo, ParkingSlotRepository slotRepo, ParkingLotRepository lotRepo) {
        this.sensorRepo = sensorRepo;
        this.slotRepo = slotRepo;
        this.lotRepo = lotRepo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<SensorDTO> getAll() {
        return sensorRepo.findAllWithSlotCode();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/unassigned")
    public List<SensorDTO> getUnassigned() {
        return sensorRepo.findAllWithSlotCode().stream()
                .filter(s -> s.getSlotId() == null)
                .toList();
    }

    @GetMapping("/config")
    public List<SensorConfigDTO> getConfigByMac(@RequestParam("mac") String mac) {
        List<SensorDTO> sensors = sensorRepo.findByMacAddress(mac).stream()
                .filter(s -> s.getStatus() != Sensor.SensorStatus.FAULTY)
                .toList();

        if (sensors.isEmpty()) {
            return Collections.emptyList();
        }

        return sensors.stream().map(s -> {
            String slotCode = s.getSlotCode() != null ? s.getSlotCode() : "A-1";

            String zone = s.getLotCode() != null
                    ? s.getLotCode()
                    : (slotCode.contains("-") ? slotCode.split("-")[0] : "A");

            String slotNum = slotCode.contains("-")
                    ? slotCode.split("-")[1].replaceFirst("^0+", "")
                    : slotCode;

            int pin = 34;
            if (s.getGpioPin() != null) {
                pin = s.getGpioPin();
            } else if (s.getSensorType() != null && s.getSensorType().name().startsWith("GPIO_")) {
                try {
                    pin = Integer.parseInt(s.getSensorType().name().replace("GPIO_", ""));
                } catch (NumberFormatException ignored) {
                }
            }

            return new SensorConfigDTO(zone, slotNum, pin);
        }).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Sensor register(@RequestBody SensorRegisterRequest req) {
        Sensor.SensorBuilder builder = Sensor.builder()
                .sensorId(UUID.randomUUID().toString())
                .macAddress(req.macAddress())
                .sensorType(req.sensorType())
                .gpioPin(req.gpioPin())
                .status(Sensor.SensorStatus.ONLINE);

        if (req.slotId() != null) {
            ParkingSlot slot = slotRepo.findById(req.slotId())
                    .orElseThrow(() -> new RuntimeException("Slot not found: " + req.slotId()));
            builder.slot(slot);
        }

        if (req.lotId() != null) {
            ParkingLot lot = lotRepo.findById(req.lotId())
                    .orElseThrow(() -> new RuntimeException("Lot not found: " + req.lotId()));
            builder.lot(lot);
        }

        return sensorRepo.save(builder.build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public Sensor updateStatus(@PathVariable String id, @RequestParam Sensor.SensorStatus status) {
        Sensor sensor = sensorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found: " + id));
        sensor.setStatus(status);
        return sensorRepo.save(sensor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/assign-slot")
    public Sensor assignSlot(@PathVariable String id, @RequestParam(required = false) Long slotId) {
        Sensor sensor = sensorRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found: " + id));

        if (slotId == null) {
            sensor.setSlot(null);
        } else {
            ParkingSlot slot = slotRepo.findById(slotId)
                    .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
            sensor.setSlot(slot);
        }
        return sensorRepo.save(sensor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        sensorRepo.deleteById(id);
    }

    public record SensorRegisterRequest(
            String macAddress,
            Sensor.SensorType sensorType,
            Long slotId,
            Integer gpioPin,
            Long lotId
    ) {}
}