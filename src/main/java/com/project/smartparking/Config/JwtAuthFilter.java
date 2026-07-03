package com.project.smartparking.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
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
   protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain)
        throws ServletException, IOException {

    System.out.println("=== JwtAuthFilter ===");
    System.out.println("Path: " + request.getRequestURI());
    System.out.println("Method: " + request.getMethod());
    System.out.println("Auth header: " + request.getHeader("Authorization"));

    String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

       if (jwtService.isTokenValid(token)) {
    String email = jwtService.extractEmail(token);
    String role = jwtService.extractRole(token);

    System.out.println("Email: " + email);
    System.out.println("Role: " + role);
    System.out.println("Authority: ROLE_" + role);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            email,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    SecurityContextHolder.getContext().setAuthentication(auth);
} else {
    System.out.println("Token is INVALID!");
}

        filterChain.doFilter(request, response);
    }
}