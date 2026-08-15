package com.project.smartparking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSettingsDTO {

    // System Settings
    private String defaultTimezone;
    private String systemCurrency;
    private Boolean is247Operation;
    private Boolean maintenanceMode;

    // Pricing Settings
    private Double bikeHourlyRate;
    private Double carHourlyRate;
    private Double suvHourlyRate;
    private Double vanHourlyRate;
    private Double dailyMaximumCap;
    private Boolean enableSurgePricing;
    private Integer surgeActivationThreshold;
    private Double maximumSurgeMultiplier;
    private Double vatRate;

    // Notification Settings
    private Boolean enableEmailNotifications;
    private String smtpServer;
    private String senderAddress;
    private Boolean enableSmsAlerts;
    private String smsGatewayProvider;
    private Boolean alertHardwareFailures;
    private Boolean alertHighCapacity;
    private Boolean alertPaymentErrors;

    // Parking Rules
    private Integer entryGracePeriodMinutes;
    private Integer exitGracePeriodMinutes;
    private Integer maximumParkingDurationHours;
    private Boolean allowOvernightParking;
    private Double overstayPenaltyRate;
    private Double lostTicketFee;
}
