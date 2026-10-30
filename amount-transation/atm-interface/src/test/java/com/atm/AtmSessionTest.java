package com.atm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atm.ui.ConsoleUI;
import com.atm.ui.InputHelper;
import com.atm.ui.Theme;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** End-to-end tests that drive the real ATM with scripted keyboard input. */
class AtmSessionTest {
    private String output;
    private int exitCode;

    private Bank run(String script, boolean ascii, boolean relogin) {
        Bank bank = Main.seedBank();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        ConsoleUI ui = new ConsoleUI(out, new Theme(false, ascii), true, false);
        InputHelper in = new InputHelper(new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), ui, null);
        exitCode = new ATM(bank, ui, in, relogin).run();
        output = buffer.toString(StandardCharsets.UTF_8);
        return bank;
    }

    private static BigDecimal bd(String s) {
        return Money.of(s);
    }

    @Test
    void threeWrongLoginsLockTheTerminal() {
        run("1001\n0000\n1001\n1111\n9999\n1234\n", false, false);
        assertEquals(ATM.EXIT_LOCKED, exitCode);
        assertTrue(output.contains("Access Denied: too many incorrect attempts"));
        assertTrue(output.contains("Attempts left"));
        assertFalse(output.contains("MAIN MENU"));
    }

    @Test
    void successfulLoginShowsMenuAndQuitShowsGoodbye() {
        run("1001\n1234\n5\n", false, false);
        assertEquals(0, exitCode);
        assertTrue(output.contains("MAIN MENU"));
        assertTrue(output.contains("XXXX1001"));
        assertTrue(output.contains("\u20B925,000.00"));
        assertTrue(output.contains("GOODBYE"));
    }

    @Test
    void emptyHistoryShowsFriendlyMessage() {
        run("1001\n1234\n1\n\n5\n", false, false);
        assertTrue(output.contains("No transactions yet in this session."));
    }

    @Test
    void withdrawFlowWithConfirmationAndReceipt() {
        Bank bank = run("1001\n1234\n2\n3\ny\n\n5\n", false, false);
        assertEquals(bd("23000.00"), bank.findAccount("1001").getBalance());
        assertTrue(output.contains("You are about to withdraw"));
        assertTrue(output.contains("Transaction Successful"));
        assertTrue(output.contains("Thank you for banking with us"));
    }

    @Test
    void withdrawDeclinedLeavesBalance() {
        Bank bank = run("1001\n1234\n2\n1\nn\n\n5\n", false, false);
        assertEquals(bd("25000.00"), bank.findAccount("1001").getBalance());
    }

    @Test
    void withdrawRejectsBadAmountsThenAcceptsGoodOne() {
        Bank bank = run("1001\n1234\n2\n5\n250\nabc\n-5\n0.00\n100.555\n200000\n300\ny\n\n5\n", false, false);
        assertEquals(bd("24700.00"), bank.findAccount("1001").getBalance());
        assertTrue(output.contains("multiples of \u20B9100"));
        assertTrue(output.contains("numbers only"));
        assertTrue(output.contains("cannot be negative"));
        assertTrue(output.contains("greater than zero"));
        assertTrue(output.contains("2 decimal places"));
        assertTrue(output.contains("maximum per transaction"));
    }

    @Test
    void insufficientFundsIsReported() {
        Bank bank = run("1003\n1111\n2\n5\n20000\n\n5\n", false, false);
        assertTrue(output.contains("Insufficient Funds"));
        assertEquals(bd("10000.00"), bank.findAccount("1003").getBalance());
    }

    @Test
    void cancelWithZeroAndBGoesBack() {
        Bank bank = run("1001\n1234\n3\n0\n\n3\nb\n\n2\n0\n\n4\nb\n\n5\n", false, false);
        assertEquals(bd("25000.00"), bank.findAccount("1001").getBalance());
        assertTrue(output.contains("Cancelled"));
    }

    @Test
    void depositFlow() {
        Bank bank = run("1001\n1234\n3\n1,500.75\n\n5\n", false, false);
        assertEquals(bd("26500.75"), bank.findAccount("1001").getBalance());
    }

    @Test
    void transferValidationAndSuccessVisibleOnBothSides() {
        Bank bank = run("1001\n1234\n4\n1001\n9999\n1002\n1000\ny\n\n5\n"
                + "1002\n4321\n1\n\n5\n", false, true);
        assertEquals(bd("24000.00"), bank.findAccount("1001").getBalance());
        assertEquals(bd("49500.00"), bank.findAccount("1002").getBalance());
        assertTrue(output.contains("cannot transfer money to your own account"));
        assertTrue(output.contains("No account found with ID 9999"));
        assertTrue(output.contains("Transfer In"));
        assertTrue(output.contains("Transfer from 1001 (Aarav Kumar)"));
    }

    @Test
    void transferWithInsufficientFundsIsBlocked() {
        Bank bank = run("1003\n1111\n4\n1001\n50000\n\n5\n", false, false);
        assertTrue(output.contains("Insufficient Funds"));
        assertEquals(bd("10000.00"), bank.findAccount("1003").getBalance());
        assertEquals(bd("25000.00"), bank.findAccount("1001").getBalance());
    }

    @Test
    void endOfInputIsHandledGracefully() {
        run("1001\n1234\n2\n", false, false);
        assertEquals(0, exitCode);
        assertTrue(output.contains("Input closed"));
        run("", false, false);
        assertTrue(output.contains("Input closed"));
    }

    @Test
    void invalidMenuInputRePrompts() {
        run("1001\n1234\nabc\n\n9\n-1\n5\n", false, false);
        assertTrue(output.contains("Please enter a number between 1 and 5."));
        assertTrue(output.contains("Please make a selection"));
    }

    @Test
    void asciiModeUsesNoNonAsciiCharacters() {
        run("1001\n1234\n3\n1000\n\n1\n\n2\n1\ny\n\n5\n", true, false);
        for (char c : output.toCharArray()) {
            assertTrue(c < 128, "Non-ASCII char in ASCII mode: U+" + Integer.toHexString(c));
        }
        assertTrue(output.contains("Rs."));
    }

    @Test
    void noLineExceedsEightyColumnsInEitherMode() {
        for (boolean ascii : new boolean[]{false, true}) {
            run("1001\n1234\n3\n99999.99\n\n2\n5\n5000\ny\n\n4\n1002\n1\ny\n\n1\n\n5\n", ascii, false);
            // Piped input echoes no Enter key, so emulate the terminal newline after each prompt.
            String screen = output.replaceAll("(?<=[^\n])(?=[\u2554\u250C]|\\+[=-]{5})", "\n");
            for (String line : screen.split("\n")) {
                String plain = line.replaceAll("\u001B\\[[;\\d]*[A-Za-z]", "");
                int cols = plain.codePointCount(0, plain.length());
                long emoji = plain.codePoints().filter(cp -> cp >= 0x1F300).count();
                assertTrue(cols + emoji <= 80, "Line too wide (" + (cols + emoji) + "): " + plain);
            }
        }
    }

    @Test
    void noEscapeCodesWhenColourIsOff() {
        run("1001\n1234\n5\n", false, false);
        assertFalse(output.contains("\u001B"));
    }
}
