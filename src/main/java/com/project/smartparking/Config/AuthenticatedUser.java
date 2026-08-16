package com.project.smartparking.Config;

import org.springframework.security.core.AuthenticatedPrincipal;

public record AuthenticatedUser(String email, Long userId, Long hubId, String role)
        implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return this.email;
    }
}