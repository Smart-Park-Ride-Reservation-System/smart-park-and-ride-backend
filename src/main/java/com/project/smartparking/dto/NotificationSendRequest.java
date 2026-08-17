package com.project.smartparking.dto;

import lombok.Data;

@Data
public class NotificationSendRequest {
    private String title;
    private String message;
    private String type; // success / payment / warning / info (display only)
    private String audience; 
    private Long hubId; 
    private Long userId; 
    private String scheduledFor;
    private boolean sendEmail; 
}