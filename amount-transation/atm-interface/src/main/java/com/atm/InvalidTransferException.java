package com.atm;

/** Thrown when a transfer request is structurally invalid (for example to self). */
public class InvalidTransferException extends AtmException {
    private static final long serialVersionUID = 1L;

    public InvalidTransferException(String message) {
        super(message);
    }
}
