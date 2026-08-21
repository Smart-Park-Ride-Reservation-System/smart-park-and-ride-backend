package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    private String status;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "reset_token")
private String resetToken;

@Column(name = "reset_token_expiry")
private Timestamp resetTokenExpiry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @ElementCollection
    @CollectionTable(name = "user_vehicles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "vehicle_no")
    private List<String> vehicles;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

    public enum Role {
        ADMIN,
        HUB_ADMIN,
        ATTENDANT,
        COMMUTER,;

        public static Role fromString(String role) {
            if (role == null)
                return COMMUTER;
            try {
                return Role.valueOf(role.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return COMMUTER;
            }
        }
    }

    @ManyToOne
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Transient
    private Boolean hasActiveSubscription;

    @Transient
    private String subscriptionLocation;
}