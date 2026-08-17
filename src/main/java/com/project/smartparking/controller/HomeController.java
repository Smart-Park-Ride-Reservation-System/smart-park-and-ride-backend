package com.project.smartparking.controller;

import com.project.smartparking.dto.HomeResponseDto;
import com.project.smartparking.dto.HubSummaryDto;
import com.project.smartparking.dto.StatDto;
import com.project.smartparking.service.HubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final HubService hubService;

    @GetMapping("/home")
    public HomeResponseDto getHomeData() {
        List<HubSummaryDto> hubs = hubService.getActiveHubSummaries();
        return HomeResponseDto.builder()
                .stats(buildStats(hubs))
                .hubs(hubs)
                .build();
    }

    private List<StatDto> buildStats(List<HubSummaryDto> hubs) {
        int totalSlots = hubs.stream().mapToInt(HubSummaryDto::getTotalSlots).sum();
        int availableSlots = hubs.stream().mapToInt(HubSummaryDto::getAvailableSlots).sum();

        return List.of(
                StatDto.builder().label("Total Slots").value(totalSlots + "+").build(),
                StatDto.builder().label("Available Now").value(String.valueOf(availableSlots)).build(),
                StatDto.builder().label("Hubs Online").value(String.valueOf(hubs.size())).build(),
                StatDto.builder().label("Uptime").value("99%").build()
        );
    }
}