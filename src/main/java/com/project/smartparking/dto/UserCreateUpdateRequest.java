package com.project.smartparking.dto;

import lombok.Data;
import com.project.smartparking.model.User.Role;

import java.util.List;

@Data
public class UserCreateUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private List<String> vehicles;
    private Role role;
    private String status;
    private Long hubId;
}