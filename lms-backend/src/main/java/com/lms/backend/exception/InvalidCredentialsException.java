package com.lms.backend.exception;

/*
 * Represents login failure

Prevents leaking sensitive info

Keeps service clean
 * CUSTOM EXCEPTION
 * ----------------
 * Thrown when login credentials are incorrect.
 */

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
