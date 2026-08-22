package com.project.smartparking.controller;

import com.project.smartparking.dto.SensorActivityEvent;
import com.project.smartparking.service.SensorActivityService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/sensors/activity")
public class SensorActivityController {

    private final SensorActivityService activityService;

    public SensorActivityController(SensorActivityService activityService) {
        this.activityService = activityService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<SensorActivityEvent> getRecent() {
        return activityService.getHistory();
    }

    // No @PreAuthorize here — SSE streams use EventSource which can't send Authorization headers.
    // URL-level security is handled by SecurityConfig.permitAll() for this path.
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return activityService.subscribe();
    }
}