package com.FraudGaurd.fraudguard_backend.controller;

import com.FraudGaurd.fraudguard_backend.dto.CreateIntegrationRequest;
import com.FraudGaurd.fraudguard_backend.dto.CreatedIntegrationResponse;
import com.FraudGaurd.fraudguard_backend.dto.IntegrationResponse;
import com.FraudGaurd.fraudguard_backend.dto.UpdateIntegrationStatusRequest;
import com.FraudGaurd.fraudguard_backend.service.ApiKeyService;
import com.FraudGaurd.fraudguard_backend.service.IntegrationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/integrations")
@SecurityRequirement(name = "adminBasicAuth")
@Tag(
        name = "Integrations",
        description = "Administrative management of Sentinel API-key integrations"
)
public class IntegrationController {

    private final IntegrationService integrationService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public IntegrationController(
            IntegrationService integrationService,
            CookieCsrfTokenRepository csrfTokenRepository) {
        this.integrationService = integrationService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf-token")
    public CsrfToken csrfToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        CsrfToken csrfToken = csrfTokenRepository.loadToken(request);
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }

        return csrfToken;
    }

    @PostMapping
    public ResponseEntity<CreatedIntegrationResponse> create(
            @Valid @RequestBody CreateIntegrationRequest request) {

        ApiKeyService.CreatedIntegration created =
                integrationService.create(request.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreatedIntegrationResponse.from(
                        created.integration(),
                        created.rawApiKey()
                ));
    }

    @PatchMapping("/{id}/status")
    public IntegrationResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIntegrationStatusRequest request) {

        return IntegrationResponse.from(
                integrationService.updateActiveStatus(id, request.getActive())
        );
    }
}
