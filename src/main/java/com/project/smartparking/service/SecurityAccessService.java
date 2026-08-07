package com.project.smartparking.service;

import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.AuthRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("securityAccess")
public class SecurityAccessService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private boolean isStaff(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_ATTENDANT"));
    }

    public boolean isOwner(Authentication authentication, Long targetUserId) {
        if (isStaff(authentication)) return true;

        String currentUserEmail = authentication.getName();
        Optional<User> targetUser = authRepository.findById(targetUserId);

        return targetUser.isPresent() && targetUser.get().getEmail().equals(currentUserEmail);
    }

    public boolean canAccessReservation(Authentication authentication, Long reservationId) {
        if (isStaff(authentication)) return true;

        String currentUserEmail = authentication.getName();
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        
        if (reservationOpt.isEmpty()) {
            return false;
        }
        
        Reservation reservation = reservationOpt.get();
        Optional<User> ownerOpt = authRepository.findById(reservation.getUserId());
        
        return ownerOpt.isPresent() && ownerOpt.get().getEmail().equals(currentUserEmail);
    }
}
