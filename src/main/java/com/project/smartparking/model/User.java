package com.project.smartparking.model;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "users")

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String status; // Active, Inactive

    @Column(name = "role")
    private String role;

    @Column(name = "created_at")
    private Timestamp createdAt;

     public enum Role {
        ADMIN,
        STAFF,
        USER;

        public static Role fromString(String role) {
            try {
                return Role.valueOf(role.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
        

    }
}