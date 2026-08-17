package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "slot_code", nullable = false, length = 20)
    private String slotCode;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED', 'OUT_OF_SERVICE')")
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;

    @OneToOne
    @JoinColumn(name = "sensor_id", referencedColumnName = "sensor_id")
    private Sensor sensor;

    public enum SlotStatus {
        AVAILABLE,
        RESERVED,
        OCCUPIED,
        OUT_OF_SERVICE
    }
}
