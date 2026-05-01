package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sensor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Long sensorId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "sensor_type")
    private String sensorType; 

    @Column(name = "mac_address", unique = true)
    private String macAddress;

    @Column(name = "last_value")
    private BigDecimal lastValue;

    private String status; // ONLINE, OFFLINE, FAULTY

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "OFFLINE"; // Default for prototype
    }
}