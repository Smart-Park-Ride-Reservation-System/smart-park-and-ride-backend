package com.project.smartparking.service;

import com.project.smartparking.dto.GlobalSettingsDTO;
import com.project.smartparking.model.GlobalSettings;
import com.project.smartparking.repository.GlobalSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {

    private final GlobalSettingsRepository globalSettingsRepository;

    public GlobalSettingsDTO getSettings() {
        GlobalSettings settings = getOrCreateSettings();
        return mapToDTO(settings);
    }

    public GlobalSettingsDTO updateSettings(GlobalSettingsDTO dto) {
        GlobalSettings settings = getOrCreateSettings();
        
        // System Settings
        if (dto.getDefaultTimezone() != null) settings.setDefaultTimezone(dto.getDefaultTimezone());
        if (dto.getSystemCurrency() != null) settings.setSystemCurrency(dto.getSystemCurrency());
        if (dto.getIs247Operation() != null) settings.setIs247Operation(dto.getIs247Operation());
        if (dto.getMaintenanceMode() != null) settings.setMaintenanceMode(dto.getMaintenanceMode());

        // Pricing Settings
        if (dto.getBikeHourlyRate() != null) settings.setBikeHourlyRate(dto.getBikeHourlyRate());
        if (dto.getCarHourlyRate() != null) settings.setCarHourlyRate(dto.getCarHourlyRate());
        if (dto.getSuvHourlyRate() != null) settings.setSuvHourlyRate(dto.getSuvHourlyRate());
        if (dto.getVanHourlyRate() != null) settings.setVanHourlyRate(dto.getVanHourlyRate());
        if (dto.getDailyMaximumCap() != null) settings.setDailyMaximumCap(dto.getDailyMaximumCap());
        if (dto.getEnableSurgePricing() != null) settings.setEnableSurgePricing(dto.getEnableSurgePricing());
        if (dto.getSurgeActivationThreshold() != null) settings.setSurgeActivationThreshold(dto.getSurgeActivationThreshold());
        if (dto.getMaximumSurgeMultiplier() != null) settings.setMaximumSurgeMultiplier(dto.getMaximumSurgeMultiplier());
        if (dto.getVatRate() != null) settings.setVatRate(dto.getVatRate());

        // Notification Settings
        if (dto.getEnableEmailNotifications() != null) settings.setEnableEmailNotifications(dto.getEnableEmailNotifications());
        if (dto.getSmtpServer() != null) settings.setSmtpServer(dto.getSmtpServer());
        if (dto.getSenderAddress() != null) settings.setSenderAddress(dto.getSenderAddress());
        if (dto.getEnableSmsAlerts() != null) settings.setEnableSmsAlerts(dto.getEnableSmsAlerts());
        if (dto.getSmsGatewayProvider() != null) settings.setSmsGatewayProvider(dto.getSmsGatewayProvider());
        if (dto.getAlertHardwareFailures() != null) settings.setAlertHardwareFailures(dto.getAlertHardwareFailures());
        if (dto.getAlertHighCapacity() != null) settings.setAlertHighCapacity(dto.getAlertHighCapacity());
        if (dto.getAlertPaymentErrors() != null) settings.setAlertPaymentErrors(dto.getAlertPaymentErrors());

        // Parking Rules
        if (dto.getEntryGracePeriodMinutes() != null) settings.setEntryGracePeriodMinutes(dto.getEntryGracePeriodMinutes());
        if (dto.getExitGracePeriodMinutes() != null) settings.setExitGracePeriodMinutes(dto.getExitGracePeriodMinutes());
        if (dto.getMaximumParkingDurationHours() != null) settings.setMaximumParkingDurationHours(dto.getMaximumParkingDurationHours());
        if (dto.getAllowOvernightParking() != null) settings.setAllowOvernightParking(dto.getAllowOvernightParking());
        if (dto.getOverstayPenaltyRate() != null) settings.setOverstayPenaltyRate(dto.getOverstayPenaltyRate());
        if (dto.getLostTicketFee() != null) settings.setLostTicketFee(dto.getLostTicketFee());

        GlobalSettings updated = globalSettingsRepository.save(settings);
        return mapToDTO(updated);
    }

    private GlobalSettings getOrCreateSettings() {
        Optional<GlobalSettings> optionalSettings = globalSettingsRepository.findById(1L);
        if (optionalSettings.isPresent()) {
            return optionalSettings.get();
        } else {
            GlobalSettings settings = GlobalSettings.builder()
                    .id(1L)
                    .defaultTimezone("Asia/Colombo (IST)")
                    .systemCurrency("LKR")
                    .is247Operation(true)
                    .maintenanceMode(false)
                    .bikeHourlyRate(50.0)
                    .carHourlyRate(100.0)
                    .suvHourlyRate(130.0)
                    .vanHourlyRate(150.0)
                    .dailyMaximumCap(1500.0)
                    .enableSurgePricing(true)
                    .surgeActivationThreshold(85)
                    .maximumSurgeMultiplier(1.5)
                    .vatRate(18.0)
                    .enableEmailNotifications(true)
                    .smtpServer("smtp.mailgun.org")
                    .senderAddress("noreply@smartpark.lk")
                    .enableSmsAlerts(true)
                    .smsGatewayProvider("Twilio")
                    .alertHardwareFailures(true)
                    .alertHighCapacity(true)
                    .alertPaymentErrors(false)
                    .entryGracePeriodMinutes(15)
                    .exitGracePeriodMinutes(20)
                    .maximumParkingDurationHours(48)
                    .allowOvernightParking(true)
                    .overstayPenaltyRate(300.0)
                    .lostTicketFee(1000.0)
                    .build();
            return globalSettingsRepository.save(settings);
        }
    }

    private GlobalSettingsDTO mapToDTO(GlobalSettings settings) {
        return GlobalSettingsDTO.builder()
                .defaultTimezone(settings.getDefaultTimezone())
                .systemCurrency(settings.getSystemCurrency())
                .is247Operation(settings.getIs247Operation())
                .maintenanceMode(settings.getMaintenanceMode())
                .bikeHourlyRate(settings.getBikeHourlyRate())
                .carHourlyRate(settings.getCarHourlyRate())
                .suvHourlyRate(settings.getSuvHourlyRate())
                .vanHourlyRate(settings.getVanHourlyRate())
                .dailyMaximumCap(settings.getDailyMaximumCap())
                .enableSurgePricing(settings.getEnableSurgePricing())
                .surgeActivationThreshold(settings.getSurgeActivationThreshold())
                .maximumSurgeMultiplier(settings.getMaximumSurgeMultiplier())
                .vatRate(settings.getVatRate())
                .enableEmailNotifications(settings.getEnableEmailNotifications())
                .smtpServer(settings.getSmtpServer())
                .senderAddress(settings.getSenderAddress())
                .enableSmsAlerts(settings.getEnableSmsAlerts())
                .smsGatewayProvider(settings.getSmsGatewayProvider())
                .alertHardwareFailures(settings.getAlertHardwareFailures())
                .alertHighCapacity(settings.getAlertHighCapacity())
                .alertPaymentErrors(settings.getAlertPaymentErrors())
                .entryGracePeriodMinutes(settings.getEntryGracePeriodMinutes())
                .exitGracePeriodMinutes(settings.getExitGracePeriodMinutes())
                .maximumParkingDurationHours(settings.getMaximumParkingDurationHours())
                .allowOvernightParking(settings.getAllowOvernightParking())
                .overstayPenaltyRate(settings.getOverstayPenaltyRate())
                .lostTicketFee(settings.getLostTicketFee())
                .build();
    }
}
