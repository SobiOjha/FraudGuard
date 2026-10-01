package com.FraudGaurd.fraudguard_backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"" + errorMessage(request) + "\"}"
        );
    }

    private String errorMessage(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/transactions")) {
            String apiKey = request.getHeader("X-API-Key");
            return apiKey == null || apiKey.isBlank()
                    ? "API key is required"
                    : "Invalid API key";
        }

        return "Authentication required";
    }
}
