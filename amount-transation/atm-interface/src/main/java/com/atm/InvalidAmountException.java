package com.atm;

/** Thrown when an amount breaks a rule (sign, decimals, limit, multiple). */
public class InvalidAmountException extends AtmException {
    private static final long serialVersionUID = 1L;

    /** Machine-readable reason so the UI can word the message itself. */
    public enum Reason { NOT_POSITIVE, TOO_MANY_DECIMALS, EXCEEDS_LIMIT, NOT_MULTIPLE }

    private final Reason reason;

    public InvalidAmountException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
