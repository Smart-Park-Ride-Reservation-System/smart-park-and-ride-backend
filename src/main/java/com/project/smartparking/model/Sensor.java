package com.project.smartparking.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sensor_mac_gpio", columnNames = {"mac_address", "gpio_pin"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @Column(name = "sensor_id", length = 50)
    private String sensorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", referencedColumnName = "slot_id", unique = true)
    @JsonIgnore
    private ParkingSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    @JsonIgnore
    private ParkingLot lot;

    @Column(name = "gpio_pin")
    private Integer gpioPin;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", length = 30)
    private SensorType sensorType;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(name = "`last_value`", precision = 10, scale = 2)
    private BigDecimal lastValue;

    @Builder.Default
    @Column(name = "is_occupied", nullable = false)
    private Boolean isOccupied = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    @Builder.Default
    private SensorStatus status = SensorStatus.OFFLINE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SensorStatus.OFFLINE;
        }
        if (this.isOccupied == null) {
            this.isOccupied = false;
        }
    }

    public boolean isOccupiedSlot() {
        return Boolean.TRUE.equals(this.isOccupied);
    }

    public enum SensorType {
        ULTRASONIC, INFRARED, MAGNETIC, CAMERA
    }

    public enum SensorStatus {
        ONLINE, OFFLINE, FAULTY
    }
}