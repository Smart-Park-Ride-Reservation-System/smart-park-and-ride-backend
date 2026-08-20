package com.project.smartparking.dto;

import com.project.smartparking.model.Sensor.SensorStatus;
import com.project.smartparking.model.Sensor.SensorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDTO {
    private String sensorId;
    private Long slotId;
    private SensorType sensorType;
    private String macAddress;
    private BigDecimal lastValue;
    private SensorStatus status;
    private LocalDateTime createdAt;
    private String slotCode;
    private Integer gpioPin;
    private Long lotId;
    private String lotCode;
    private String lotName;
}