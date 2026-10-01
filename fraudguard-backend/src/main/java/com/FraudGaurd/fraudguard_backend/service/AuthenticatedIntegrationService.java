package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidApiKeyException;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Provides the Integration authenticated by the X-API-Key security filter.
 */
@Service
public class AuthenticatedIntegrationService {

    public Integration getRequiredIntegration() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Integration integration)) {
            throw new InvalidApiKeyException();
        }

        return integration;
    }
}
