package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "global_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSettings {

    @Id
    @Column(name = "id")
    private Long id = 1L; // Singleton pattern

    // System Settings
    @Column(name = "default_timezone")
    private String defaultTimezone;

    @Column(name = "system_currency")
    private String systemCurrency;

    @Column(name = "is_24_7_operation")
    private Boolean is247Operation;

    @Column(name = "maintenance_mode")
    private Boolean maintenanceMode;

    // Pricing Settings
    @Column(name = "bike_hourly_rate")
    private Double bikeHourlyRate;

    @Column(name = "car_hourly_rate")
    private Double carHourlyRate;

    @Column(name = "suv_hourly_rate")
    private Double suvHourlyRate;

    @Column(name = "van_hourly_rate")
    private Double vanHourlyRate;

    @Column(name = "daily_maximum_cap")
    private Double dailyMaximumCap;

    @Column(name = "enable_surge_pricing")
    private Boolean enableSurgePricing;

    @Column(name = "surge_activation_threshold")
    private Integer surgeActivationThreshold;

    @Column(name = "maximum_surge_multiplier")
    private Double maximumSurgeMultiplier;

    @Column(name = "vat_rate")
    private Double vatRate;

    // Notification Settings
    @Column(name = "enable_email_notifications")
    private Boolean enableEmailNotifications;

    @Column(name = "smtp_server")
    private String smtpServer;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "enable_sms_alerts")
    private Boolean enableSmsAlerts;

    @Column(name = "sms_gateway_provider")
    private String smsGatewayProvider;

    @Column(name = "alert_hardware_failures")
    private Boolean alertHardwareFailures;

    @Column(name = "alert_high_capacity")
    private Boolean alertHighCapacity;

    @Column(name = "alert_payment_errors")
    private Boolean alertPaymentErrors;

    // Parking Rules
    @Column(name = "entry_grace_period_minutes")
    private Integer entryGracePeriodMinutes;

    @Column(name = "exit_grace_period_minutes")
    private Integer exitGracePeriodMinutes;

    @Column(name = "maximum_parking_duration_hours")
    private Integer maximumParkingDurationHours;

    @Column(name = "allow_overnight_parking")
    private Boolean allowOvernightParking;

    @Column(name = "overstay_penalty_rate")
    private Double overstayPenaltyRate;

    @Column(name = "lost_ticket_fee")
    private Double lostTicketFee;
}
