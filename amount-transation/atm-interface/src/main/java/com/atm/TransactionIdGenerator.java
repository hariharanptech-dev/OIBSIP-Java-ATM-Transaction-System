package com.atm;

import java.util.concurrent.atomic.AtomicLong;

/** Produces unique, sequential ids such as TXN-000001. Shared by a whole bank. */
public class TransactionIdGenerator {
    private final AtomicLong counter = new AtomicLong();

    /** @return the next id in the sequence */
    public String next() {
        return String.format("TXN-%06d", counter.incrementAndGet());
    }
}
