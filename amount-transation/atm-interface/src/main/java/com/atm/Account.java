package com.atm;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * A bank account. The balance can only change through deposit/withdraw or the
 * package-private transfer hooks used by {@link Bank}; there is no balance setter.
 * Contains no console I/O so it is fully unit-testable.
 */
public class Account {
    /** Per-transaction ceiling for deposits, withdrawals and transfers. */
    public static final BigDecimal MAX_TXN_AMOUNT = Money.of("100000.00");
    /** Withdrawals must be a multiple of this value. */
    public static final BigDecimal WITHDRAW_MULTIPLE = new BigDecimal("100");
    /** Required PIN length. */
    public static final int PIN_LENGTH = 4;

    private final String accountId;
    private final String holderName;
    private final String pinHash;
    private final TransactionIdGenerator idGenerator;
    private final List<Transaction> transactions = new ArrayList<>();
    private BigDecimal balance;

    /**
     * @param accountId      1-12 letters/digits
     * @param holderName     non-blank display name
     * @param pin            4 digits (stored only as a SHA-256 hash)
     * @param openingBalance non-negative, at most 2 decimals
     * @param idGenerator    shared generator so ids are unique across accounts
     */
    public Account(String accountId, String holderName, String pin,
                   BigDecimal openingBalance, TransactionIdGenerator idGenerator) {
        if (accountId == null || !accountId.matches("[A-Za-z0-9]{1,12}")) {
            throw new IllegalArgumentException("Account id must be 1-12 letters or digits");
        }
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("Holder name is required");
        }
        if (pin == null || !pin.matches("\\d{" + PIN_LENGTH + "}")) {
            throw new IllegalArgumentException("PIN must be exactly " + PIN_LENGTH + " digits");
        }
        if (openingBalance == null || openingBalance.signum() < 0
                || Money.hasMoreThanTwoDecimals(openingBalance)) {
            throw new IllegalArgumentException("Opening balance must be >= 0 with at most 2 decimals");
        }
        if (idGenerator == null) {
            throw new IllegalArgumentException("Id generator is required");
        }
        this.accountId = accountId;
        this.holderName = holderName.trim();
        this.pinHash = hashPin(accountId, pin);
        this.balance = Money.normalize(openingBalance);
        this.idGenerator = idGenerator;
    }

    /** Convenience constructor with a private id generator (handy in tests). */
    public Account(String accountId, String holderName, String pin, BigDecimal openingBalance) {
        this(accountId, holderName, pin, openingBalance, new TransactionIdGenerator());
    }

    // ------------------------------------------------------------------ PIN

    /** SHA-256 of "accountId:pin" as lowercase hex. Salting with the id defeats rainbow tables. */
    public static String hashPin(String accountId, String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((accountId + ":" + pin).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    /** Constant-time PIN check. */
    public boolean verifyPin(String pin) {
        if (pin == null) {
            return false;
        }
        return MessageDigest.isEqual(
                pinHash.getBytes(StandardCharsets.UTF_8),
                hashPin(accountId, pin).getBytes(StandardCharsets.UTF_8));
    }

    /** @return the stored hash (never the PIN itself) */
    public String getPinHash() {
        return pinHash;
    }

    // ----------------------------------------------------------- validation

    private static void validateBase(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException(InvalidAmountException.Reason.NOT_POSITIVE,
                    "Amount must be greater than zero");
        }
        if (Money.hasMoreThanTwoDecimals(amount)) {
            throw new InvalidAmountException(InvalidAmountException.Reason.TOO_MANY_DECIMALS,
                    "Amount can have at most 2 decimal places");
        }
        if (amount.compareTo(MAX_TXN_AMOUNT) > 0) {
            throw new InvalidAmountException(InvalidAmountException.Reason.EXCEEDS_LIMIT,
                    "Amount exceeds the per-transaction limit of " + MAX_TXN_AMOUNT);
        }
    }

    /** Positive, at most 2 decimals, within the per-transaction limit. */
    public static void validateDepositAmount(BigDecimal amount) {
        validateBase(amount);
    }

    /** Same as deposit rules plus "multiple of 100". */
    public static void validateWithdrawalAmount(BigDecimal amount) {
        validateBase(amount);
        if (amount.remainder(WITHDRAW_MULTIPLE).signum() != 0) {
            throw new InvalidAmountException(InvalidAmountException.Reason.NOT_MULTIPLE,
                    "Amount must be a multiple of " + WITHDRAW_MULTIPLE);
        }
    }

    /** Transfers follow the deposit rules. */
    public static void validateTransferAmount(BigDecimal amount) {
        validateBase(amount);
    }

    /** @throws InsufficientFundsException when the balance is lower than the amount */
    public void requireSufficientFunds(BigDecimal amount) {
        BigDecimal needed = Money.normalize(amount);
        if (balance.compareTo(needed) < 0) {
            throw new InsufficientFundsException(balance, needed);
        }
    }

    // ----------------------------------------------------------- operations

    /** Adds money and logs a DEPOSIT. */
    public Transaction deposit(BigDecimal amount) {
        validateDepositAmount(amount);
        BigDecimal value = Money.normalize(amount);
        balance = balance.add(value);
        return record(TransactionType.DEPOSIT, value, "Cash deposit");
    }

    /** Removes money and logs a WITHDRAWAL. */
    public Transaction withdraw(BigDecimal amount) {
        validateWithdrawalAmount(amount);
        BigDecimal value = Money.normalize(amount);
        requireSufficientFunds(value);
        balance = balance.subtract(value);
        return record(TransactionType.WITHDRAWAL, value, "Cash withdrawal");
    }

    /** Debit side of a transfer; called only by {@link Bank#transfer}. */
    Transaction debitForTransfer(BigDecimal amount, String peerId, String peerName) {
        BigDecimal value = Money.normalize(amount);
        requireSufficientFunds(value);
        balance = balance.subtract(value);
        return record(TransactionType.TRANSFER_OUT, value, "Transfer to " + peerId + " (" + peerName + ")");
    }

    /** Credit side of a transfer; called only by {@link Bank#transfer}. */
    Transaction creditForTransfer(BigDecimal amount, String peerId, String peerName) {
        BigDecimal value = Money.normalize(amount);
        balance = balance.add(value);
        return record(TransactionType.TRANSFER_IN, value, "Transfer from " + peerId + " (" + peerName + ")");
    }

    /** Reverses the most recent transaction (used for transfer rollback). */
    void undo(Transaction tx) {
        int last = transactions.size() - 1;
        if (last < 0 || transactions.get(last) != tx) {
            throw new IllegalStateException("Only the latest transaction can be undone");
        }
        transactions.remove(last);
        balance = tx.getType().isCredit() ? balance.subtract(tx.getAmount()) : balance.add(tx.getAmount());
    }

    private Transaction record(TransactionType type, BigDecimal amount, String description) {
        Transaction tx = new Transaction(idGenerator.next(), LocalDateTime.now(), type,
                amount, balance, description);
        transactions.add(tx);
        return tx;
    }

    // -------------------------------------------------------------- getters

    public String getAccountId() { return accountId; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }

    /** @return read-only view, oldest transaction first */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /** @return id masked like XXXX1001 */
    public String getMaskedId() {
        String tail = accountId.length() > 4 ? accountId.substring(accountId.length() - 4) : accountId;
        return "XXXX" + tail;
    }
}
