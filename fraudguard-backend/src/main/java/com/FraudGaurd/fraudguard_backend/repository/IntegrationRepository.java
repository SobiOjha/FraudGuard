package com.FraudGaurd.fraudguard_backend.repository;

import com.FraudGaurd.fraudguard_backend.model.Integration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationRepository extends JpaRepository<Integration, Long> {

    Optional<Integration> findByApiKeyHashAndActiveTrue(String apiKeyHash);
}
