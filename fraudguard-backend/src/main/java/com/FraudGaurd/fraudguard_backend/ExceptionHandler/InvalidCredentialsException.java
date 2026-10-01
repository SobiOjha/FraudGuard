package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
