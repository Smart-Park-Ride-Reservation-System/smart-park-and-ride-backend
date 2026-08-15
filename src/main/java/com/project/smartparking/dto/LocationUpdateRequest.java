package com.project.smartparking.dto;

import lombok.Data;

@Data
public class LocationUpdateRequest {
    private Long hubId;
    private Long lotId;
    private Long slotId;
}
