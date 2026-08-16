package com.project.smartparking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorDTO(
        Long sensorId,
        Long slotId,
        String sensorType,
        String macAddress,
        BigDecimal lastValue,
        String status,
        LocalDateTime createdAt,
        String slotCode
) {}