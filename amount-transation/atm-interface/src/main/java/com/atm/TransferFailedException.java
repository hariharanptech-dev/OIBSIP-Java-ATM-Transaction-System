package com.atm;

/** Thrown when a transfer failed half-way and was rolled back. */
public class TransferFailedException extends AtmException {
    private static final long serialVersionUID = 1L;

    public TransferFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
