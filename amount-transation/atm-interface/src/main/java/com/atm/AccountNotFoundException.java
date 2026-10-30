package com.atm;

/** Thrown when an account id does not exist in the bank. */
public class AccountNotFoundException extends AtmException {
    private static final long serialVersionUID = 1L;

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
