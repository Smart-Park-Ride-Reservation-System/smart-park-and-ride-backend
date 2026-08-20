package com.project.smartparking.dto;

import com.project.smartparking.model.ParkingLot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotDto {
    private Long lotId;
    private String name;
    private Long hubId;

    public static ParkingLotDto from(ParkingLot lot) {
        return new ParkingLotDto(
                lot.getLotId(),
                lot.getName(),
                lot.getHub() != null ? lot.getHub().getHubId() : null
        );
    }
}