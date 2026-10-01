package com.FraudGaurd.fraudguard_backend.service;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidApiKeyException;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.repository.IntegrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private IntegrationRepository integrationRepository;

    @Test
    void createIntegrationGeneratesAndStoresOnlyAHash() {
        when(integrationRepository.save(any(Integration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyService service = new ApiKeyService(integrationRepository);
        ApiKeyService.CreatedIntegration created =
                service.createIntegration("Demo Payment System");

        ArgumentCaptor<Integration> captor =
                ArgumentCaptor.forClass(Integration.class);
        verify(integrationRepository).save(captor.capture());

        Integration saved = captor.getValue();
        assertNotNull(created.rawApiKey());
        assertTrue(created.rawApiKey().length() >= 40);
        assertNotEquals(created.rawApiKey(), saved.getApiKeyHash());
        assertTrue(saved.getApiKeyHash().matches("[0-9a-f]{64}"));
        assertEquals("Demo Payment System", saved.getName());
        assertTrue(saved.isActive());
    }

    @Test
    void generatedKeysAreDifferent() {
        when(integrationRepository.save(any(Integration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyService service = new ApiKeyService(integrationRepository);

        String first = service.createIntegration("First").rawApiKey();
        String second = service.createIntegration("Second").rawApiKey();

        assertNotEquals(first, second);
    }

    @Test
    void authenticateLooksUpTheHashOfTheRawKey() {
        when(integrationRepository.findByApiKeyHashAndActiveTrue(any()))
                .thenReturn(Optional.of(new Integration("Demo", "hash")));

        ApiKeyService service = new ApiKeyService(integrationRepository);

        assertNotNull(service.authenticate("a-valid-looking-key"));
        verify(integrationRepository)
                .findByApiKeyHashAndActiveTrue(service.hash("a-valid-looking-key"));
    }

    @Test
    void authenticateRejectsMissingOrInactiveKeys() {
        when(integrationRepository.findByApiKeyHashAndActiveTrue(any()))
                .thenReturn(Optional.empty());

        ApiKeyService service = new ApiKeyService(integrationRepository);

        assertThrows(
                InvalidApiKeyException.class,
                () -> service.authenticate(null)
        );
        assertThrows(
                InvalidApiKeyException.class,
                () -> service.authenticate("inactive-key")
        );
    }
}
