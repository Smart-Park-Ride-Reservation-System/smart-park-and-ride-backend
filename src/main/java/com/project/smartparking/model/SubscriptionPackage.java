package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g. "Daily Pass", "Weekly Package", "Monthly Package"

    @Column(nullable = false)
    private Integer durationDays; // 1, 7, 30

    @Column(nullable = false)
    private BigDecimal price; // 500.00, 2500.00, 9000.00

    @Column(nullable = false)
    private boolean active = true;

    private String description;
}
