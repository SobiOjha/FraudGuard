package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class InvalidFraudActionException extends RuntimeException {

    public InvalidFraudActionException(String message) {
        super(message);
    }
}