package com.atm;

/** Both sides of a completed transfer. */
public record TransferReceipt(Transaction debit, Transaction credit) {
}
