package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class IntegrationNotFoundException extends RuntimeException {

    public IntegrationNotFoundException(Long id) {
        super("Integration not found with ID: " + id);
    }
}
