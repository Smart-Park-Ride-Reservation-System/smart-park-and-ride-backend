package com.project.smartparking.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hub {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hub_id")
    private Long hub_id;

    @Column(name = "name")
    private String name;

    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "city")
    private String city;

    @Column(name = "gps_location")
    private String gps_location; // Stored as "lat,long" string
}