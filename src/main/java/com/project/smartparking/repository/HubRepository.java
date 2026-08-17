package com.project.smartparking.repository;

import com.project.smartparking.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HubRepository extends JpaRepository<Hub, Long> {

    List<Hub> findByStatus(String status);
}