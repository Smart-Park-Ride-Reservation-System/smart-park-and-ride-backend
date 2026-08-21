package com.project.smartparking.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.smartparking.model.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole() != null ? user.getRole().name() : "USER")
                .claim("userId", user.getUserId())
                .claim("firstName", user.getFirstName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration));

        if (user.getHub() != null) {
            builder.claim("hubId", user.getHub().getHubId());
        }

        return builder.signWith(getKey()).compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);

    }

    public Long extractHubId(String token) {
        Object hubIdObj = getClaims(token).get("hubId");
        if (hubIdObj == null) return null;
        return ((Number) hubIdObj).longValue();
    }

    public Long extractUserId(String token) {
        Object userIdObj = getClaims(token).get("userId");
        if (userIdObj == null) return null;
        return ((Number) userIdObj).longValue();
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}