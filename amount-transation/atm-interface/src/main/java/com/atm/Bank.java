package com.atm;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Holds all accounts, authenticates users and performs atomic transfers. No console I/O. */
public class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final TransactionIdGenerator idGenerator = new TransactionIdGenerator();

    /** @return the generator shared by every account of this bank */
    public TransactionIdGenerator getIdGenerator() {
        return idGenerator;
    }

    /** Creates and registers a new account. */
    public Account openAccount(String id, String holderName, String pin, BigDecimal openingBalance) {
        Account account = new Account(id, holderName, pin, openingBalance, idGenerator);
        registerAccount(account);
        return account;
    }

    /** Registers an existing account object. */
    public void registerAccount(Account account) {
        if (accounts.putIfAbsent(account.getAccountId(), account) != null) {
            throw new IllegalArgumentException("Duplicate account id: " + account.getAccountId());
        }
    }

    /** @return read-only view of all accounts */
    public Map<String, Account> getAccounts() {
        return Collections.unmodifiableMap(accounts);
    }

    /** @throws AccountNotFoundException if the id is unknown */
    public Account findAccount(String id) {
        Account account = id == null ? null : accounts.get(id);
        if (account == null) {
            throw new AccountNotFoundException(id);
        }
        return account;
    }

    /**
     * Verifies credentials. The failure message is identical for unknown ids and wrong
     * PINs so attackers cannot discover which ids exist.
     */
    public Account authenticate(String userId, String pin) {
        Account account = userId == null ? null : accounts.get(userId);
        if (account == null || !account.verifyPin(pin)) {
            throw new AuthenticationException("Incorrect User ID or PIN");
        }
        return account;
    }

    /**
     * Moves money atomically. If the credit fails after the debit succeeded, the debit is
     * rolled back and a {@link TransferFailedException} is thrown.
     */
    public synchronized TransferReceipt transfer(String fromId, String toId, BigDecimal amount) {
        Account.validateTransferAmount(amount);
        Account from = findAccount(fromId);
        Account to = findAccount(toId);
        if (from == to) {
            throw new InvalidTransferException("You cannot transfer money to your own account");
        }
        BigDecimal value = Money.normalize(amount);
        Transaction debit = from.debitForTransfer(value, to.getAccountId(), to.getHolderName());
        try {
            Transaction credit = to.creditForTransfer(value, from.getAccountId(), from.getHolderName());
            return new TransferReceipt(debit, credit);
        } catch (RuntimeException e) {
            from.undo(debit);
            throw new TransferFailedException("Transfer failed and was rolled back", e);
        }
    }
}
