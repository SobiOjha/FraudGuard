package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.IntegrationNotFoundException;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.repository.IntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationService {

    private final ApiKeyService apiKeyService;
    private final IntegrationRepository integrationRepository;

    public IntegrationService(
            ApiKeyService apiKeyService,
            IntegrationRepository integrationRepository) {
        this.apiKeyService = apiKeyService;
        this.integrationRepository = integrationRepository;
    }

    public ApiKeyService.CreatedIntegration create(String name) {
        return apiKeyService.createIntegration(name);
    }

    @Transactional
    public Integration updateActiveStatus(Long id, boolean active) {
        Integration integration = integrationRepository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException(id));

        integration.setActive(active);
        return integrationRepository.save(integration);
    }
}
