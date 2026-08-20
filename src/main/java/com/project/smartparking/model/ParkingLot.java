package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(
    name = "parking_lots",
    uniqueConstraints = {
        // Enforces uniqueness of lotCode ONLY within the same Hub
        @UniqueConstraint(name = "uk_hub_lot_code", columnNames = {"hub_id", "lot_code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    // Fixed: Changed from EAGER to LAZY to avoid unnecessary DB joins
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hub_id", nullable = false)
    private Hub hub;

    @Enumerated(EnumType.STRING)
    @Column(name = "lot_type")
    @Builder.Default
    private LotType lotType = LotType.GROUND;

    // Fixed: Removed global `unique = true` here
    @Column(name = "lot_code", nullable = false, length = 20)
    private String lotCode;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private LotStatus status = LotStatus.ACTIVE;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    // Added: Parent-child connection to slots
    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSlot> slots;

    public enum LotType {
        GROUND, BASEMENT, MULTI_LEVEL, VIP
    }

    public enum LotStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}