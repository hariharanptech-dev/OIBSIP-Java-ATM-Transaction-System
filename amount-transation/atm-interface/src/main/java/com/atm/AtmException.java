package com.atm;

/** Base class of all business-rule failures raised by the ATM domain model. */
public class AtmException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AtmException(String message) {
        super(message);
    }

    public AtmException(String message, Throwable cause) {
        super(message, cause);
    }
}
