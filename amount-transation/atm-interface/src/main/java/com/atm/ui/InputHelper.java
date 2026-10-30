package com.atm.ui;

import java.io.Console;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Scanner;

/**
 * Safe, validating input. Every read loops until the input is valid, never throws on bad
 * text, and signals end-of-file with {@link InputClosedException}. Typing "0" or "b" at
 * cancellable prompts returns an empty Optional.
 */
public class InputHelper {
    private final Scanner scanner;
    private final ConsoleUI ui;
    private final Console console;

    /**
     * @param in      the input stream (System.in)
     * @param ui      used to print prompts and inline errors
     * @param console a real terminal console, or null to use the Scanner fallback
     *                (PIN then cannot be masked)
     */
    public InputHelper(InputStream in, ConsoleUI ui, Console console) {
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
        this.ui = ui;
        this.console = console;
    }

    /** @return true if PIN entry can be hidden */
    public boolean isMaskingAvailable() {
        return console != null;
    }

    private String readLine() {
        String line;
        if (console != null) {
            line = console.readLine();
        } else {
            line = scanner.hasNextLine() ? scanner.nextLine() : null;
        }
        if (line == null) {
            throw new InputClosedException();
        }
        return line.trim();
    }

    private static boolean isCancel(String s) {
        return s.equals("0") || s.equalsIgnoreCase("b");
    }

    /** Login User ID (no cancel). */
    public String readUserId() {
        while (true) {
            ui.prompt("User ID", "");
            String s = readLine();
            if (s.isEmpty()) {
                ui.error("Please enter your User ID.");
            } else if (!s.matches("[A-Za-z0-9]{1,12}")) {
                ui.error("User ID may contain letters and digits only (max 12 characters).");
            } else {
                return s;
            }
        }
    }

    /** Login PIN, masked when a terminal console exists. */
    public String readPin() {
        while (true) {
            ui.prompt("PIN    ", console != null ? "4 digits, hidden" : "4 digits, visible here");
            String s;
            if (console != null) {
                char[] chars = console.readPassword();
                if (chars == null) {
                    throw new InputClosedException();
                }
                s = new String(chars).trim();
                java.util.Arrays.fill(chars, ' ');
            } else {
                s = readLine();
            }
            if (!s.matches("\\d{4}")) {
                ui.error("PIN must be exactly 4 digits.");
            } else {
                return s;
            }
        }
    }

    /**
     * Reads an integer in [min, max].
     *
     * @return the choice, or empty if cancel is allowed and the user typed 0 / b
     */
    public OptionalInt readChoice(String label, int min, int max, boolean allowCancel) {
        String hint = min + "-" + max + (allowCancel ? ", 0/b to cancel" : "");
        while (true) {
            ui.prompt(label, hint);
            String s = readLine();
            if (allowCancel && isCancel(s)) {
                return OptionalInt.empty();
            }
            if (s.isEmpty()) {
                ui.error("Please make a selection (" + min + "-" + max + ").");
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return OptionalInt.of(v);
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            ui.error("Please enter a number between " + min + " and " + max + ".");
        }
    }

    /** Reads a money amount; empty means the user cancelled. */
    public Optional<BigDecimal> readAmount(String label) {
        while (true) {
            ui.prompt(label, "0/b to cancel");
            String s = readLine();
            if (isCancel(s)) {
                return Optional.empty();
            }
            try {
                return Optional.of(parseAmount(s));
            } catch (IllegalArgumentException e) {
                ui.error(e.getMessage());
            }
        }
    }

    /**
     * Parses user text into a positive scale-2 amount.
     *
     * @throws IllegalArgumentException with a polite, user-facing message
     */
    static BigDecimal parseAmount(String raw) {
        String s = raw.replace(",", "").replace("\u20B9", "").replaceAll("(?i)^rs\\.?", "").trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        if (s.startsWith("-")) {
            throw new IllegalArgumentException("Amount cannot be negative. Please enter a positive amount.");
        }
        if (!s.matches("\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Please enter numbers only, for example 1500 or 1500.50.");
        }
        String integerPart = s.split("\\.")[0].replaceFirst("^0+(?=\\d)", "");
        if (integerPart.length() > 9) {
            throw new IllegalArgumentException("That amount is too large. Please enter a smaller amount.");
        }
        BigDecimal v = new BigDecimal(s);
        if (v.signum() != 0 && v.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amounts can have at most 2 decimal places.");
        }
        if (v.signum() == 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /** Reads an account id; empty means cancelled. */
    public Optional<String> readAccountId(String label) {
        while (true) {
            ui.prompt(label, "0/b to cancel");
            String s = readLine();
            if (isCancel(s)) {
                return Optional.empty();
            }
            if (s.isEmpty()) {
                ui.error("Please enter the recipient's account ID.");
            } else if (!s.matches("[A-Za-z0-9]{1,12}")) {
                ui.error("Account IDs contain letters and digits only (max 12 characters).");
            } else {
                return Optional.of(s);
            }
        }
    }

    /** Asks a Y/N question; empty input is re-asked (no unsafe default). */
    public boolean readYesNo(String label) {
        while (true) {
            ui.prompt(label, "Y/N");
            String s = readLine().toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
            ui.error("Please type Y for yes or N for no.");
        }
    }

    /** Waits for the user to press Enter. */
    public void waitForEnter(String message) {
        ui.promptEnter(message);
        readLine();
    }
}
