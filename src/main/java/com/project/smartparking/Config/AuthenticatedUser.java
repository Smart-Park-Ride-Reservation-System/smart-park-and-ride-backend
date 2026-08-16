package com.project.smartparking.Config;

public record AuthenticatedUser(String email, Long userId, Long hubId, String role) {}