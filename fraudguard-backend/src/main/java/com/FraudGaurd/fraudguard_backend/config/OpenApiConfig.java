package com.FraudGaurd.fraudguard_backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecuritySchemes({
        @SecurityScheme(
                name = "apiKeyAuth",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-API-Key",
                description = "API key issued to an external Sentinel integration"
        ),
        @SecurityScheme(
                name = "adminBasicAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "basic",
                description = "HTTP Basic credentials for an ADMIN user"
        )
})
public class OpenApiConfig {
}
