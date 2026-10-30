package com.atm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankTest {
    private Bank bank;
    private Account a;
    private Account b;

    private static BigDecimal bd(String s) {
        return Money.of(s);
    }

    @BeforeEach
    void setUp() {
        bank = Main.seedBank();
        a = bank.findAccount("1001");
        b = bank.findAccount("1002");
    }

    @Test
    void authenticateSucceedsWithCorrectCredentials() {
        assertSame(a, bank.authenticate("1001", "1234"));
    }

    @Test
    void authenticateFailsForWrongPinOrUnknownUser() {
        assertThrows(AuthenticationException.class, () -> bank.authenticate("1001", "0000"));
        assertThrows(AuthenticationException.class, () -> bank.authenticate("9999", "1234"));
        assertThrows(AuthenticationException.class, () -> bank.authenticate(null, null));
    }

    @Test
    void findAccountThrowsForUnknownId() {
        assertThrows(AccountNotFoundException.class, () -> bank.findAccount("nope"));
    }

    @Test
    void duplicateAccountIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> bank.openAccount("1001", "Dup", "1234", bd("1")));
    }

    @Test
    void transferSuccessUpdatesBothAccountsAndHistories() {
        TransferReceipt r = bank.transfer("1001", "1002", bd("1000.25"));
        assertEquals(bd("23999.75"), a.getBalance());
        assertEquals(bd("49500.25"), b.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, a.getTransactions().get(0).getType());
        assertEquals(TransactionType.TRANSFER_IN, b.getTransactions().get(0).getType());
        assertSame(r.debit(), a.getTransactions().get(0));
        assertSame(r.credit(), b.getTransactions().get(0));
        assertNotEquals(r.debit().getId(), r.credit().getId());
    }

    @Test
    void transferWithInsufficientFundsChangesNothing() {
        assertThrows(InsufficientFundsException.class, () -> bank.transfer("1003", "1001", bd("10000.01")));
        assertEquals(bd("10000.00"), bank.findAccount("1003").getBalance());
        assertEquals(bd("25000.00"), a.getBalance());
        assertTrue(bank.findAccount("1003").getTransactions().isEmpty());
    }

    @Test
    void selfTransferIsRejected() {
        assertThrows(InvalidTransferException.class, () -> bank.transfer("1001", "1001", bd("100")));
        assertEquals(bd("25000.00"), a.getBalance());
    }

    @Test
    void unknownRecipientIsRejected() {
        assertThrows(AccountNotFoundException.class, () -> bank.transfer("1001", "7777", bd("100")));
        assertThrows(AccountNotFoundException.class, () -> bank.transfer("7777", "1001", bd("100")));
        assertEquals(bd("25000.00"), a.getBalance());
    }

    @Test
    void invalidTransferAmountsAreRejected() {
        assertThrows(InvalidAmountException.class, () -> bank.transfer("1001", "1002", bd("0")));
        assertThrows(InvalidAmountException.class, () -> bank.transfer("1001", "1002", bd("-1")));
        assertThrows(InvalidAmountException.class, () -> bank.transfer("1001", "1002", bd("100000.01")));
    }

    @Test
    void failedCreditRollsBackTheDebit() {
        Account faulty = new Account("9001", "Faulty", "1111", bd("0"), bank.getIdGenerator()) {
            @Override
            Transaction creditForTransfer(BigDecimal amount, String peerId, String peerName) {
                throw new IllegalStateException("boom");
            }
        };
        bank.registerAccount(faulty);
        assertThrows(TransferFailedException.class, () -> bank.transfer("1001", "9001", bd("500")));
        assertEquals(bd("25000.00"), a.getBalance());
        assertTrue(a.getTransactions().isEmpty());
        assertEquals(bd("0.00"), faulty.getBalance());
    }

    @Test
    void transactionIdsAreUniqueAcrossAccounts() {
        a.deposit(bd("100"));
        b.deposit(bd("100"));
        bank.transfer("1001", "1002", bd("50"));
        bank.findAccount("1003").withdraw(bd("100"));
        Set<String> ids = new HashSet<>();
        int total = 0;
        for (Account acc : bank.getAccounts().values()) {
            for (Transaction t : acc.getTransactions()) {
                ids.add(t.getId());
                total++;
            }
        }
        assertEquals(5, total);
        assertEquals(total, ids.size());
    }

    @Test
    void sessionSummaryTotalsAreCorrect() {
        a.deposit(bd("1000"));
        a.withdraw(bd("300"));
        bank.transfer("1001", "1002", bd("200"));
        bank.transfer("1002", "1001", bd("50"));
        SessionSummary s = SessionSummary.of(a.getTransactions());
        assertEquals(4, s.count());
        assertEquals(bd("1000.00"), s.deposited());
        assertEquals(bd("300.00"), s.withdrawn());
        assertEquals(bd("200.00"), s.transferredOut());
        assertEquals(bd("50.00"), s.transferredIn());
        assertEquals(bd("550.00"), s.netChange());
    }
}
