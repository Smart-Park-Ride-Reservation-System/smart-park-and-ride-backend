package com.project.smartparking.dto;

import lombok.Data;

@Data
public class NotificationSendRequest {
    private String title;
    private String message;
    private String type;        // success / payment / warning / info (display only)
    private String audience;    // ALL_USERS / HUB / SINGLE_USER
    private Long hubId;         // present only if audience == HUB
    private Long userId;        // present only if audience == SINGLE_USER
    private String scheduledFor; // ISO datetime string, null if sending now
}