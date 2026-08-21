package com.project.smartparking.dto;

import lombok.Data;

@Data
public class WalkInRequest {
    private Long slotId;
    private String vehicleNo;
    private String vehicleType;
}