package com.project.smartparking.repository;

import com.project.smartparking.model.SubscriptionPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionPackageRepository extends JpaRepository<SubscriptionPackage, Long> {
    List<SubscriptionPackage> findByActiveTrue();
}
