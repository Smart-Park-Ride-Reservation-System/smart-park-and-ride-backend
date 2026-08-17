package com.project.smartparking.service;

import com.project.smartparking.Config.AuthenticatedUser;
import com.project.smartparking.model.Reservation;
import com.project.smartparking.model.User;
import com.project.smartparking.repository.AuthRepository;
import com.project.smartparking.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service("securityAccess")
public class SecurityAccessService {

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final Set<String> STAFF_ROLES = Set.of(
            "ADMIN", "ROLE_ADMIN",
            "HUB_ADMIN", "ROLE_HUB_ADMIN",
            "ATTENDANT", "ROLE_ATTENDANT");

    private boolean isStaff(Authentication authentication) {
        if (authentication == null)
            return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_ROLES::contains);
    }

    private String getEmailFromAuth(Authentication authentication) {
        if (authentication == null)
            return null;
        if (authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getEmail();
        }
        return authentication.getName();
    }

    public boolean isOwner(Authentication authentication, Long targetUserId) {
        if (isStaff(authentication))
            return true;

        String currentUserEmail = getEmailFromAuth(authentication);
        if (currentUserEmail == null)
            return false;

        Optional<User> targetUser = authRepository.findById(targetUserId);
        return targetUser.isPresent() && targetUser.get().getEmail().equals(currentUserEmail);
    }

    public boolean canAccessReservation(Authentication authentication, Long reservationId) {
        if (isStaff(authentication))
            return true;

        String currentUserEmail = getEmailFromAuth(authentication);
        if (currentUserEmail == null)
            return false;

        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            return false;
        }

        Reservation reservation = reservationOpt.get();
        Optional<User> ownerOpt = authRepository.findById(reservation.getUserId());
        return ownerOpt.isPresent() && ownerOpt.get().getEmail().equals(currentUserEmail);
    }
}