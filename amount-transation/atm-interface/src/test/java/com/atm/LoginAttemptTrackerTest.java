package com.atm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptTrackerTest {
    @Test
    void locksAfterThreeFailures() {
        LoginAttemptTracker t = new LoginAttemptTracker();
        assertEquals(3, t.remaining());
        t.recordFailure();
        assertEquals(2, t.remaining());
        assertFalse(t.isLocked());
        t.recordFailure();
        assertFalse(t.isLocked());
        t.recordFailure();
        assertTrue(t.isLocked());
        assertEquals(0, t.remaining());
        t.recordFailure();
        assertEquals(3, t.getFailures());
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> new LoginAttemptTracker(0));
    }

    @Test
    void transactionTypeFlagsAreConsistent() {
        assertTrue(TransactionType.DEPOSIT.isCredit());
        assertTrue(TransactionType.TRANSFER_IN.isCredit());
        assertFalse(TransactionType.WITHDRAWAL.isCredit());
        assertFalse(TransactionType.TRANSFER_OUT.isCredit());
    }
}
