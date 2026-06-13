package com.lms.backend.exception;

/*
 * Represents a business rule violation
 * Keeps service logic clean
 * Allows meaningful error messages to client
 * 
 * CUSTOM EXCEPTION
 * ----------------
 * Thrown when a user tries to register
 * with an email that already exists.
 */

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}