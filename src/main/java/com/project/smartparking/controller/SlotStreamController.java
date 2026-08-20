package com.project.smartparking.controller;

import com.project.smartparking.service.SlotStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/slots")
public class SlotStreamController {

    @Autowired
    private SlotStreamService slotStreamService;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@RequestParam Long hubId, @RequestParam Long lotId) {
        return slotStreamService.subscribe(hubId, lotId);
    }
}