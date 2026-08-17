package com.project.smartparking.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HubSummaryDto {
    private Long hubId;
    private String name;
    private String city;
    private String province;
    private String mapUrl;      
    private String status;
    private int availableSlots; 
    private int totalSlots;     
}