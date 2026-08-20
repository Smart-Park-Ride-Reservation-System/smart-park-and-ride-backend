package com.project.smartparking.dto;

import com.project.smartparking.model.ParkingSlot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotSummaryDto {
    private Long slotId;
    private String slotCode;

    public static SlotSummaryDto from(ParkingSlot slot) {
        if (slot == null) return null;
        return new SlotSummaryDto(slot.getSlotId(), slot.getSlotCode());
    }
}