package com.project.smartparking.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponseDto {
    private List<StatDto> stats;
    private List<HubSummaryDto> hubs;
}