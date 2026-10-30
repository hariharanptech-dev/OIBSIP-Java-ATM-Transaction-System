package com.atm;

import com.atm.ui.ConsoleUI;
import com.atm.ui.InputHelper;
import com.atm.ui.Theme;

import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Entry point: parses flags, builds the Bank, seeds demo data and starts the ATM. */
public final class Main {
    private static final List<String> KNOWN_FLAGS =
            List.of("--no-color", "--color", "--ascii", "--fast", "--relogin", "--help");

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        List<String> flags = Arrays.asList(args);
        boolean interactive = Theme.isInteractiveTerminal();

        Theme theme = Theme.detect(flags.contains("--no-color"), flags.contains("--color"),
                flags.contains("--ascii"), interactive);
        ConsoleUI ui = new ConsoleUI(out, theme, flags.contains("--fast"), interactive);

        if (flags.contains("--help") || !KNOWN_FLAGS.containsAll(flags)) {
            ui.showUsage();
            if (!flags.contains("--help")) {
                System.exit(2);
            }
            return;
        }

        Console console = interactive ? System.console() : null;
        InputHelper input = new InputHelper(System.in, ui, console);
        int code = new ATM(seedBank(), ui, input, flags.contains("--relogin")).run();
        if (code != 0) {
            System.exit(code);
        }
    }

    /** Builds the demo bank. */
    static Bank seedBank() {
        Bank bank = new Bank();
        bank.openAccount("1001", "Aarav Kumar", "1234", new BigDecimal("25000.00"));
        bank.openAccount("1002", "Priya Sharma", "4321", new BigDecimal("48500.00"));
        bank.openAccount("1003", "Rahul Verma", "1111", new BigDecimal("10000.00"));
        bank.openAccount("1004", "Sneha Iyer", "2222", new BigDecimal("75250.50"));
        return bank;
    }
}
