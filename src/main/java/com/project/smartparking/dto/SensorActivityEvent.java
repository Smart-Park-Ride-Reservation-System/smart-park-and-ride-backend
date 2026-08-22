package com.project.smartparking.dto;

import java.time.LocalDateTime;

public record SensorActivityEvent(
        LocalDateTime timestamp,
        String level,     // INFO, WARN, ERROR
        String message
) {}