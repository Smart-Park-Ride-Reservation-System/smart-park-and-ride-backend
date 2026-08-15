package com.project.smartparking.service;

import com.project.smartparking.dto.SubscriptionPurchaseRequest;
import com.project.smartparking.model.SubscriptionPackage;
import com.project.smartparking.model.User;
import com.project.smartparking.model.UserSubscription;
import com.project.smartparking.model.Hub;
import com.project.smartparking.model.ParkingLot;
import com.project.smartparking.model.ParkingSlot;
import com.project.smartparking.repository.SubscriptionPackageRepository;
import com.project.smartparking.repository.UserRepository;
import com.project.smartparking.repository.UserSubscriptionRepository;
import com.project.smartparking.repository.HubRepository;
import com.project.smartparking.repository.ParkingLotRepository;
import com.project.smartparking.repository.ParkingSlotRepository;
import com.project.smartparking.dto.LocationUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.project.smartparking.dto.SubscriptionSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

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

    public List<SubscriptionPackage> getAllPackages() {
        return packageRepository.findAll();
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

    public SubscriptionSummaryResponse getSubscriptionSummary() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<UserSubscription> activeSubs = userSubscriptionRepository.findCurrentlyActive(now);

        BigDecimal mrr = activeSubs.stream()
                .map(sub -> {
                    SubscriptionPackage pkg = sub.getSubscriptionPackage();
                    if (pkg == null || pkg.getDurationDays() == null || pkg.getDurationDays() <= 0) {
                        return BigDecimal.ZERO;
                    }
                    // Normalize each plan's price to a monthly-equivalent so a
                    // daily pass and a monthly plan aren't just added at face value.
                    return pkg.getPrice()
                            .multiply(BigDecimal.valueOf(30))
                            .divide(BigDecimal.valueOf(pkg.getDurationDays()), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SubscriptionSummaryResponse.builder()
                .activeCount(activeSubs.size())
                .mrr(mrr)
                .build();
    }

    public List<UserSubscription> getAllSubscriptions() {
        return userSubscriptionRepository.findAll();
    }

    public UserSubscription updateLocation(Long userId, LocationUpdateRequest request) {
        UserSubscription activeSub = getActiveSubscription(userId);
        if (activeSub == null) {
            throw new RuntimeException("No active subscription found");
        }
        if (request.getHubId() != null) {
            Hub hub = hubRepository.findById(request.getHubId())
                    .orElseThrow(() -> new RuntimeException("Hub not found"));
            activeSub.setHub(hub);
        } else {
            activeSub.setHub(null);
        }

        if (request.getLotId() != null) {
            ParkingLot lot = parkingLotRepository.findById(request.getLotId())
                    .orElseThrow(() -> new RuntimeException("Lot not found"));
            activeSub.setLot(lot);
        } else {
            activeSub.setLot(null);
        }

        if (request.getSlotId() != null) {
            ParkingSlot slot = parkingSlotRepository.findById(request.getSlotId())
                    .orElseThrow(() -> new RuntimeException("Slot not found"));
            activeSub.setSlot(slot);
        } else {
            activeSub.setSlot(null);
        }

        return userSubscriptionRepository.save(activeSub);
    }
}