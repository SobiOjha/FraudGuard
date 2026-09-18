package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }
}