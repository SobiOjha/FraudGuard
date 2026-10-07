package com.FraudGaurd.fraudguard_backend.security;

import com.FraudGaurd.fraudguard_backend.ExceptionHandler.InvalidApiKeyException;
import com.FraudGaurd.fraudguard_backend.model.Integration;
import com.FraudGaurd.fraudguard_backend.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ApiKeyAuthenticationFilter(
            ApiKeyService apiKeyService,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.apiKeyService = apiKeyService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/health")
                || path.startsWith("/api/auth/")
                || path.equals("/api/auth")
                || path.startsWith("/api/integrations")
                || path.startsWith("/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String rawApiKey = request.getHeader("X-API-Key");

        if (rawApiKey == null || rawApiKey.isBlank()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidApiKeyExceptionAdapter()
            );
            return;
        }

        try {
            Integration integration = apiKeyService.authenticate(rawApiKey);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            integration,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTEGRATION"))
                    )
            );
            SecurityContextHolder.setContext(context);

            filterChain.doFilter(request, response);
        } catch (InvalidApiKeyException exception) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidApiKeyExceptionAdapter()
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static class InvalidApiKeyExceptionAdapter
            extends org.springframework.security.core.AuthenticationException {

        private InvalidApiKeyExceptionAdapter() {
            super("Invalid API key");
        }
    }
}
