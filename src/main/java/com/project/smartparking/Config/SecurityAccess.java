package com.project.smartparking.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.project.smartparking.repository.ReservationRepository;
import com.project.smartparking.repository.UserRepository;

@Component("securityAccess")
public class SecurityAccess {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public boolean isOwner(Authentication authentication, Long userId) {
        if (authentication == null || userId == null) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_ATTENDANT"));
        if (isStaff) {
            return true;
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> user.getUserId().equals(userId))
                .orElse(false);
    }

    public boolean canAccessReservation(Authentication authentication, Long reservationId) {
        if (authentication == null || reservationId == null) {
            return false;
        }

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_ATTENDANT"));
        if (isStaff) {
            return true;
        }

        return reservationRepository.findById(reservationId)
                .map(reservation -> {
                    String email = authentication.getName();
                    return userRepository.findByEmail(email)
                            .map(user -> user.getUserId().equals(reservation.getUserId()))
                            .orElse(false);
                })
                .orElse(false);
    }
}