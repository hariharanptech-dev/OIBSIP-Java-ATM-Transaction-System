package com.atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable record of one balance-changing event. */
public final class Transaction {
    private final String id;
    private final LocalDateTime timestamp;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;

    public Transaction(String id, LocalDateTime timestamp, TransactionType type,
                       BigDecimal amount, BigDecimal balanceAfter, String description) {
        this.id = Objects.requireNonNull(id);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.type = Objects.requireNonNull(type);
        this.amount = Money.normalize(Objects.requireNonNull(amount));
        this.balanceAfter = Money.normalize(Objects.requireNonNull(balanceAfter));
        this.description = description == null ? "" : description;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return id + " " + type + " " + amount + " -> " + balanceAfter;
    }
}
