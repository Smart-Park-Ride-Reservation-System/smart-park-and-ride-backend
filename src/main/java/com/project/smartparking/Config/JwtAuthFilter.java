package com.project.smartparking.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.smartparking.service.JwtService;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 1. If missing or not Bearer, proceed as unauthenticated
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        // 2. Ignore invalid string values passed by frontend
        if (token.isEmpty() || "undefined".equalsIgnoreCase(token) || "null".equalsIgnoreCase(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                String rawRole = jwtService.extractRole(token);
                Long userId = jwtService.extractUserId(token);
                Long hubId = jwtService.extractHubId(token);

                // Ensure non-null clean role string
                String cleanRole = (rawRole != null && !rawRole.isBlank()) ? rawRole.trim() : "COMMUTER";
                
                // Strip "ROLE_" prefix for store consistency in principal if needed
                String normalizedRole = cleanRole.startsWith("ROLE_") ? cleanRole.substring(5) : cleanRole;
                
                // Standard authority string MUST retain "ROLE_" prefix for Spring Security hasRole()
                String authorityName = cleanRole.startsWith("ROLE_") ? cleanRole : "ROLE_" + cleanRole;

                AuthenticatedUser principal = new AuthenticatedUser(email, userId, hubId, normalizedRole);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(authorityName)));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            logger.warn("JWT validation failed: " + e.getMessage());
            SecurityContextHolder.clearContext(); // Explicitly clear unauthenticated context on error
        }

        filterChain.doFilter(request, response);
    }
}