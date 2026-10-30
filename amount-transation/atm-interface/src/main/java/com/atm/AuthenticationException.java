package com.atm;

/** Thrown when the User ID / PIN combination is wrong. Deliberately vague. */
public class AuthenticationException extends AtmException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
