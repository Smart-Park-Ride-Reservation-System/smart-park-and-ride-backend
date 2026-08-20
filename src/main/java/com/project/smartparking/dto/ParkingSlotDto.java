package com.project.smartparking.dto;

import com.project.smartparking.model.ParkingSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlotDto {
    private Long slotId;
    private String slotCode;
    private ParkingSlot.SlotStatus status;
    private Long lotId;
    private String lotName;
    private Long hubId;
    private String hubName;
    private SensorInfo sensor;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorInfo {
        private String sensorId;
        private String macAddress;
    }

    public static ParkingSlotDto from(ParkingSlot slot) {
        Long lotId = null, hubId = null;
        String lotName = null, hubName = null;
        SensorInfo sensorInfo = null;

        if (slot.getLot() != null) {
            lotId = slot.getLot().getLotId();
            lotName = slot.getLot().getName();
            if (slot.getLot().getHub() != null) {
                hubId = slot.getLot().getHub().getHubId();
                hubName = slot.getLot().getHub().getName();
            }
        }

        if (slot.getSensor() != null) {
            sensorInfo = new SensorInfo(
                    slot.getSensor().getSensorId(),
                    slot.getSensor().getMacAddress()
            );
        }

        return new ParkingSlotDto(
                slot.getSlotId(), slot.getSlotCode(), slot.getStatus(),
                lotId, lotName, hubId, hubName,
                sensorInfo
        );
    }
}