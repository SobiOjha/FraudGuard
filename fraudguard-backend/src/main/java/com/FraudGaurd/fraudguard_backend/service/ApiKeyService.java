package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidApiKeyException;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.repository.IntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiKeyService {

    private static final int API_KEY_BYTES = 32;

    private final IntegrationRepository integrationRepository;
    private final SecureRandom secureRandom;

    @Autowired
    public ApiKeyService(IntegrationRepository integrationRepository) {
        this(integrationRepository, new SecureRandom());
    }

    ApiKeyService(
            IntegrationRepository integrationRepository,
            SecureRandom secureRandom) {
        this.integrationRepository = integrationRepository;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public CreatedIntegration createIntegration(String name) {
        String rawApiKey = generateApiKey();
        // API keys are high-entropy secrets, so a deterministic SHA-256 hash
        // allows indexed lookup without persisting the raw key.
        Integration integration = new Integration(name, hash(rawApiKey));
        Integration savedIntegration = integrationRepository.save(integration);

        return new CreatedIntegration(savedIntegration, rawApiKey);
    }

    @Transactional(readOnly = true)
    public Integration authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new InvalidApiKeyException();
        }

        return integrationRepository
                .findByApiKeyHashAndActiveTrue(hash(rawApiKey))
                .orElseThrow(InvalidApiKeyException::new);
    }

    String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    String hash(String rawApiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    public record CreatedIntegration(
            Integration integration,
            String rawApiKey) {
    }
}
