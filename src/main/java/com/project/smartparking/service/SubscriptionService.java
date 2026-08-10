package com.project.smartparking.service;

import com.project.smartparking.dto.SubscriptionPurchaseRequest;
import com.project.smartparking.model.SubscriptionPackage;
import com.project.smartparking.model.User;
import com.project.smartparking.model.UserSubscription;
import com.project.smartparking.repository.SubscriptionPackageRepository;
import com.project.smartparking.repository.UserRepository;
import com.project.smartparking.repository.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionPackageRepository packageRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    // ------------------------------------------------------------
    // USER-FACING METHODS
    // ------------------------------------------------------------

    public List<SubscriptionPackage> getActivePackages() {
        return packageRepository.findByActiveTrue();
    }

    public List<UserSubscription> getUserSubscriptions(Long userId) {
        return userSubscriptionRepository.findByUser_UserId(userId);
    }

    public UserSubscription getActiveSubscription(Long userId) {
        Optional<UserSubscription> activeSub = userSubscriptionRepository
                .findFirstByUser_UserIdAndStatusOrderByEndDateDesc(userId, "ACTIVE");

        if (activeSub.isPresent()) {
            UserSubscription sub = activeSub.get();
            if (sub.getEndDate().before(new Timestamp(System.currentTimeMillis()))) {
                sub.setStatus("EXPIRED");
                userSubscriptionRepository.save(sub);
                return null;
            }
            return sub;
        }
        return null;
    }

    public UserSubscription purchasePackage(SubscriptionPurchaseRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPackage subPackage = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new RuntimeException("Package not found"));

        UserSubscription existing = getActiveSubscription(user.getUserId());
        if (existing != null) {
            existing.setStatus("EXPIRED");
            userSubscriptionRepository.save(existing);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(subPackage.getDurationDays());

        UserSubscription newSubscription = UserSubscription.builder()
                .user(user)
                .subscriptionPackage(subPackage)
                .startDate(Timestamp.valueOf(now))
                .endDate(Timestamp.valueOf(end))
                .status("ACTIVE")
                .build();

        return userSubscriptionRepository.save(newSubscription);
    }

    // ------------------------------------------------------------
    // ADMIN-FACING METHODS
    // ------------------------------------------------------------

    public List<SubscriptionPackage> getAllPackages() {
        return packageRepository.findAll(); // admin sees active + inactive
    }

    public SubscriptionPackage createPackage(SubscriptionPackage pkg) {
        return packageRepository.save(pkg);
    }

    public SubscriptionPackage updatePackage(Long id, SubscriptionPackage updated) {
        SubscriptionPackage existing = packageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        existing.setName(updated.getName());
        existing.setDurationDays(updated.getDurationDays());
        existing.setPrice(updated.getPrice());
        existing.setDescription(updated.getDescription());
        existing.setActive(updated.isActive());

        return packageRepository.save(existing);
    }

    public void deletePackage(Long id) {
        if (!packageRepository.existsById(id)) {
            throw new RuntimeException("Package not found");
        }
        packageRepository.deleteById(id);
    }
}