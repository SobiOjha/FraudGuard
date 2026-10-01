package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException() {
        super("Invalid API key");
    }
}
