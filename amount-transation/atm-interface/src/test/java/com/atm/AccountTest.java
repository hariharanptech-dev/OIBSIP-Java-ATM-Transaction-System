package com.atm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountTest {
    private Account account;

    private static BigDecimal bd(String s) {
        return Money.of(s);
    }

    @BeforeEach
    void setUp() {
        account = new Account("1001", "Aarav Kumar", "1234", bd("25000.00"));
    }

    @Test
    @DisplayName("deposit increases balance and logs a DEPOSIT")
    void depositWorks() {
        Transaction tx = account.deposit(bd("500.50"));
        assertEquals(bd("25500.50"), account.getBalance());
        assertEquals(TransactionType.DEPOSIT, tx.getType());
        assertEquals(bd("25500.50"), tx.getBalanceAfter());
        assertEquals(1, account.getTransactions().size());
    }

    @Test
    void withdrawWorks() {
        Transaction tx = account.withdraw(bd("2000"));
        assertEquals(bd("23000.00"), account.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, tx.getType());
    }

    @Test
    void withdrawExactBalanceLeavesZero() {
        Account a = new Account("2001", "Zed", "1111", bd("300"));
        a.withdraw(bd("300"));
        assertEquals(bd("0.00"), a.getBalance());
    }

    @Test
    void insufficientFundsLeavesStateUntouched() {
        InsufficientFundsException e = assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(bd("30000")));
        assertEquals(bd("25000.00"), e.getAvailable());
        assertEquals(bd("25000.00"), account.getBalance());
        assertTrue(account.getTransactions().isEmpty());
    }

    @Test
    void invalidDepositAmountsAreRejected() {
        assertEquals(InvalidAmountException.Reason.NOT_POSITIVE,
                assertThrows(InvalidAmountException.class, () -> account.deposit(bd("0"))).getReason());
        assertEquals(InvalidAmountException.Reason.NOT_POSITIVE,
                assertThrows(InvalidAmountException.class, () -> account.deposit(bd("-5"))).getReason());
        assertEquals(InvalidAmountException.Reason.NOT_POSITIVE,
                assertThrows(InvalidAmountException.class, () -> account.deposit(null)).getReason());
        assertEquals(InvalidAmountException.Reason.TOO_MANY_DECIMALS,
                assertThrows(InvalidAmountException.class, () -> account.deposit(new BigDecimal("1.005"))).getReason());
        assertEquals(InvalidAmountException.Reason.EXCEEDS_LIMIT,
                assertThrows(InvalidAmountException.class, () -> account.deposit(bd("100000.01"))).getReason());
        assertEquals(bd("25000.00"), account.getBalance());
    }

    @Test
    void depositAtLimitIsAllowed() {
        account.deposit(Account.MAX_TXN_AMOUNT);
        assertEquals(bd("125000.00"), account.getBalance());
    }

    @Test
    void withdrawalMustBeMultipleOf100() {
        assertEquals(InvalidAmountException.Reason.NOT_MULTIPLE,
                assertThrows(InvalidAmountException.class, () -> account.withdraw(bd("150"))).getReason());
        assertEquals(InvalidAmountException.Reason.NOT_MULTIPLE,
                assertThrows(InvalidAmountException.class, () -> account.withdraw(bd("100.50"))).getReason());
        account.withdraw(bd("100"));
    }

    @Test
    void withdrawalLimitIsEnforced() {
        Account rich = new Account("3001", "Rich", "1111", bd("500000"));
        rich.withdraw(bd("100000"));
        assertEquals(InvalidAmountException.Reason.EXCEEDS_LIMIT,
                assertThrows(InvalidAmountException.class, () -> rich.withdraw(bd("100100"))).getReason());
    }

    @Test
    void pinIsHashedNotStored() {
        String hash = account.getPinHash();
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertFalse(hash.contains("1234"));
        assertEquals(hash, Account.hashPin("1001", "1234"));
        assertNotEquals(hash, Account.hashPin("1002", "1234"));
        assertTrue(account.verifyPin("1234"));
        assertFalse(account.verifyPin("4321"));
        assertFalse(account.verifyPin(null));
    }

    @Test
    void constructorValidatesInput() {
        assertThrows(IllegalArgumentException.class, () -> new Account("", "A", "1234", bd("1")));
        assertThrows(IllegalArgumentException.class, () -> new Account("1", " ", "1234", bd("1")));
        assertThrows(IllegalArgumentException.class, () -> new Account("1", "A", "12", bd("1")));
        assertThrows(IllegalArgumentException.class, () -> new Account("1", "A", "abcd", bd("1")));
        assertThrows(IllegalArgumentException.class, () -> new Account("1", "A", "1234", bd("-1")));
    }

    @Test
    void maskedIdAndHistoryAreProtected() {
        assertEquals("XXXX1001", account.getMaskedId());
        account.deposit(bd("100"));
        assertThrows(UnsupportedOperationException.class, () -> account.getTransactions().clear());
    }

    @Test
    void historyIsOrderedOldestFirstWithSequentialIds() {
        account.deposit(bd("100"));
        account.withdraw(bd("200"));
        account.deposit(bd("300"));
        var list = account.getTransactions();
        assertEquals("TXN-000001", list.get(0).getId());
        assertEquals("TXN-000002", list.get(1).getId());
        assertEquals("TXN-000003", list.get(2).getId());
        assertEquals(TransactionType.WITHDRAWAL, list.get(1).getType());
    }
}
