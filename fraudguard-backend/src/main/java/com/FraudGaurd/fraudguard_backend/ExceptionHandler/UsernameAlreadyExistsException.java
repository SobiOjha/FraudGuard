package com.FraudGaurd.fraudguard_backend.ExceptionHandler;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("Username already exists");
    }
}
