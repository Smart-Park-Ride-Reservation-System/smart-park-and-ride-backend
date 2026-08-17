package com.project.smartparking.dto;

public record SensorConfigDTO(
        String zone,
        String slot,
        Integer pin
) {}