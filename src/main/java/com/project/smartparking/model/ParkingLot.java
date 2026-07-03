package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Enumerated(EnumType.STRING)
    @Column(name = "lot_type")
    @Builder.Default
    private LotType lotType = LotType.GROUND;

    @Column(name = "lot_code", nullable = false, unique = true, length = 20)
    private String lotCode;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private LotStatus status = LotStatus.ACTIVE;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    public enum LotType {
        GROUND, BASEMENT, MULTI_LEVEL, VIP
    }

    public enum LotStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}