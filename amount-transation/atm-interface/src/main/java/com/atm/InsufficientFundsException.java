package com.atm;

import java.math.BigDecimal;

/** Thrown when the balance cannot cover a withdrawal or outgoing transfer. */
public class InsufficientFundsException extends AtmException {
    private static final long serialVersionUID = 1L;

    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super("Insufficient Funds: available " + available + ", requested " + requested);
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
