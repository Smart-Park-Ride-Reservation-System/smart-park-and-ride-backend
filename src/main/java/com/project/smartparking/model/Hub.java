package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transport_hubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hub_id")
    private Long hubId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "province", length = 50)
    private String province;

    @Column(name = "district", length = 50)
    private String district;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "gps_location", length = 255)
    private String gpsLocation; // Format: "lat,long"

    @Column(name = "status", length = 30)
    private String status; 
}
