package com.atm;

/** Kinds of transaction recorded in an account history. */
public enum TransactionType {
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_IN("Transfer In", true),
    TRANSFER_OUT("Transfer Out", false);

    private final String label;
    private final boolean credit;

    TransactionType(String label, boolean credit) {
        this.label = label;
        this.credit = credit;
    }

    /** @return short human-readable label (max 12 characters) */
    public String getLabel() {
        return label;
    }

    /** @return true if this type increases the balance */
    public boolean isCredit() {
        return credit;
    }
}
