package com.atm;

import com.atm.ui.ConsoleUI;
import com.atm.ui.InputClosedException;
import com.atm.ui.InputHelper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Session flow: login, menu loop and delegation to {@link Bank}/{@link Account}.
 * All printing goes through {@link ConsoleUI}; all reading through {@link InputHelper}.
 */
public class ATM {
    /** Name shown on the welcome screen. */
    public static final String BANK_NAME = "BHARAT TRUST BANK";
    /** Exit code when the terminal is locked after failed logins. */
    public static final int EXIT_LOCKED = 1;

    private static final BigDecimal[] QUICK_AMOUNTS = {
        Money.of("500"), Money.of("1000"), Money.of("2000"), Money.of("5000")};
    private static final String BACK = "Press Enter to return to the menu";

    private final Bank bank;
    private final ConsoleUI ui;
    private final InputHelper in;
    private final boolean relogin;

    /**
     * @param relogin when true, Quit returns to the login screen instead of ending the program
     */
    public ATM(Bank bank, ConsoleUI ui, InputHelper in, boolean relogin) {
        this.bank = bank;
        this.ui = ui;
        this.in = in;
        this.relogin = relogin;
    }

    /** Runs the application. @return 0 on normal exit, {@link #EXIT_LOCKED} after lock-out */
    public int run() {
        try {
            ui.showWelcome(BANK_NAME);
            do {
                Account account = login();
                if (account == null) {
                    return EXIT_LOCKED;
                }
                menuLoop(account);
            } while (relogin);
            return 0;
        } catch (InputClosedException e) {
            ui.showInputClosed();
            return 0;
        }
    }

    /** @return the authenticated account, or null after 3 failed attempts */
    private Account login() {
        LoginAttemptTracker tracker = new LoginAttemptTracker();
        ui.showLoginHeader(in.isMaskingAvailable());
        while (true) {
            String id = in.readUserId();
            String pin = in.readPin();
            try {
                Account account = bank.authenticate(id, pin);
                ui.showLoginSuccess(account);
                return account;
            } catch (AuthenticationException e) {
                tracker.recordFailure();
                ui.showLoginFailure(tracker.remaining(), tracker.getMaxAttempts());
                if (tracker.isLocked()) {
                    ui.showLockScreen();
                    return null;
                }
            }
        }
    }

    private void menuLoop(Account account) {
        while (true) {
            ui.showMenu(account);
            int choice = in.readChoice("Select an option", 1, 5, false).getAsInt();
            switch (choice) {
                case 1 -> history(account);
                case 2 -> withdraw(account);
                case 3 -> deposit(account);
                case 4 -> transfer(account);
                case 5 -> {
                    ui.showGoodbye(account, SessionSummary.of(account.getTransactions()));
                    return;
                }
                default -> ui.error("Invalid option.");
            }
        }
    }

    private void history(Account account) {
        ui.showHistory(account);
        in.waitForEnter("Press Enter to return");
    }

    private void cancelled() {
        ui.warn("Cancelled. Nothing was changed.");
        in.waitForEnter(BACK);
    }

    /** Re-prompts until the amount passes the validator; empty means the user cancelled. */
    private Optional<BigDecimal> readValidAmount(String label, Consumer<BigDecimal> validator) {
        while (true) {
            Optional<BigDecimal> amount = in.readAmount(label);
            if (amount.isEmpty()) {
                return amount;
            }
            try {
                validator.accept(amount.get());
                return amount;
            } catch (InvalidAmountException e) {
                ui.showAmountError(e);
            }
        }
    }

    private void withdraw(Account account) {
        ui.showWithdrawScreen(account, QUICK_AMOUNTS);
        OptionalInt pick = in.readChoice("Choose an amount", 1, QUICK_AMOUNTS.length + 1, true);
        if (pick.isEmpty()) {
            cancelled();
            return;
        }
        BigDecimal amount;
        if (pick.getAsInt() <= QUICK_AMOUNTS.length) {
            amount = QUICK_AMOUNTS[pick.getAsInt() - 1];
        } else {
            Optional<BigDecimal> entered = readValidAmount("Enter amount", Account::validateWithdrawalAmount);
            if (entered.isEmpty()) {
                cancelled();
                return;
            }
            amount = entered.get();
        }
        try {
            account.requireSufficientFunds(amount);
            ui.showConfirmation("You are about to withdraw " + ui.money(amount),
                    List.of(new String[]{"Amount", ui.money(amount)},
                            new String[]{"Balance after", ui.money(account.getBalance().subtract(amount))}));
            if (!in.readYesNo("Confirm?")) {
                cancelled();
                return;
            }
            Transaction tx = account.withdraw(amount);
            ui.showReceipt(tx, account);
        } catch (InsufficientFundsException e) {
            ui.showInsufficientFunds(e);
        } catch (AtmException e) {
            ui.error(e.getMessage());
        }
        in.waitForEnter(BACK);
    }

    private void deposit(Account account) {
        ui.showDepositScreen(account);
        Optional<BigDecimal> amount = readValidAmount("Amount to deposit", Account::validateDepositAmount);
        if (amount.isEmpty()) {
            cancelled();
            return;
        }
        try {
            ui.showReceipt(account.deposit(amount.get()), account);
        } catch (AtmException e) {
            ui.error(e.getMessage());
        }
        in.waitForEnter(BACK);
    }

    private void transfer(Account account) {
        ui.showTransferScreen(account);
        Account target = null;
        while (target == null) {
            Optional<String> id = in.readAccountId("Recipient account ID");
            if (id.isEmpty()) {
                cancelled();
                return;
            }
            if (id.get().equals(account.getAccountId())) {
                ui.error("You cannot transfer money to your own account.");
                continue;
            }
            try {
                target = bank.findAccount(id.get());
            } catch (AccountNotFoundException e) {
                ui.error("No account found with ID " + id.get() + ". Please check the number and try again.");
            }
        }
        ui.success("Recipient: " + target.getHolderName() + " (" + target.getMaskedId() + ")");
        Optional<BigDecimal> amount = readValidAmount("Amount to transfer", Account::validateTransferAmount);
        if (amount.isEmpty()) {
            cancelled();
            return;
        }
        try {
            account.requireSufficientFunds(amount.get());
            ui.showConfirmation("You are about to transfer " + ui.money(amount.get()),
                    List.of(new String[]{"To", target.getHolderName()},
                            new String[]{"Recipient account", target.getMaskedId()},
                            new String[]{"Amount", ui.money(amount.get())},
                            new String[]{"Balance after", ui.money(account.getBalance().subtract(amount.get()))}));
            if (!in.readYesNo("Confirm?")) {
                cancelled();
                return;
            }
            TransferReceipt receipt = bank.transfer(account.getAccountId(), target.getAccountId(), amount.get());
            ui.showReceipt(receipt.debit(), account);
        } catch (InsufficientFundsException e) {
            ui.showInsufficientFunds(e);
        } catch (AtmException e) {
            ui.error(e.getMessage());
        }
        in.waitForEnter(BACK);
    }
}
