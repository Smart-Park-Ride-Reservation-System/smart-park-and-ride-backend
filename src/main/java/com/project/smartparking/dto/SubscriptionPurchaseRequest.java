package com.project.smartparking.dto;

import lombok.Data;

@Data
public class SubscriptionPurchaseRequest {
    private Long userId;
    private Long packageId;
    private Long hubId;
    private Long lotId;
    private Long slotId;
    private String email;
}
