package com.project.smartparking.Config;

import com.project.smartparking.model.SubscriptionPackage;
import com.project.smartparking.repository.SubscriptionPackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PackageSeeder implements CommandLineRunner {

    @Autowired
    private SubscriptionPackageRepository packageRepo;

    @Override
    public void run(String... args) throws Exception {
        if (packageRepo.count() == 0) {
            packageRepo.save(SubscriptionPackage.builder()
                    .name("Daily Pass")
                    .durationDays(1)
                    .price(new BigDecimal("500.00"))
                    .description("Access to parking for 24 hours")
                    .active(true)
                    .build());

            packageRepo.save(SubscriptionPackage.builder()
                    .name("Weekly Pass")
                    .durationDays(7)
                    .price(new BigDecimal("2500.00"))
                    .description("Standard weekly pass for commuters")
                    .active(true)
                    .build());

            packageRepo.save(SubscriptionPackage.builder()
                    .name("Monthly Pass")
                    .durationDays(30)
                    .price(new BigDecimal("9000.00"))
                    .description("Best value for everyday commuters")
                    .active(true)
                    .build());
            
            System.out.println("Default Subscription Packages Seeded.");
        }
    }
}
