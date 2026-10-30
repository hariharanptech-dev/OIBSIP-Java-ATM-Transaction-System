package com.atm.ui;

import com.atm.Account;
import com.atm.InsufficientFundsException;
import com.atm.InvalidAmountException;
import com.atm.SessionSummary;
import com.atm.Transaction;
import com.atm.ui.Theme.Box;
import com.atm.ui.Theme.BoxStyle;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Every character the application prints goes through this class: banners, panels,
 * menus, tables, receipts, spinner and messages. Colours and symbols come from {@link Theme}.
 */
public class ConsoleUI {
    /** Width of standard panels. */
    public static final int WIDTH = 60;
    private static final int RECEIPT_WIDTH = 50;
    private static final String SEP = "\u0000SEP";

    private static final DateTimeFormatter LONG_DT =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy  HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter MENU_DT =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter RECEIPT_DT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    private static final String[] BANNER = {
        " ____    _    _   _ _  __       _  _____ __  __",
        "| __ )  / \\  | \\ | | |/ /      / \\|_   _|  \\/  |",
        "|  _ \\ / _ \\ |  \\| | ' /      / _ \\ | | | |\\/| |",
        "| |_) / ___ \\| |\\  | . \\     / ___ \\| | | |  | |",
        "|____/_/   \\_\\_| \\_|_|\\_\\   /_/   \\_\\_| |_|  |_|",
    };

    private static final String[] MENU_LABELS = {
        "Transaction History", "Withdraw", "Deposit", "Transfer", "Quit"};

    private final PrintStream out;
    private final Theme t;
    private final CurrencyFormatter fmt;
    private final boolean fast;
    private final boolean animate;
    private final boolean clearEnabled;

    /**
     * @param out         UTF-8 print stream
     * @param theme       colours/symbols
     * @param fast        skip animations
     * @param interactive true when attached to a real terminal (enables animation and clearing)
     */
    public ConsoleUI(PrintStream out, Theme theme, boolean fast, boolean interactive) {
        this.out = out;
        this.t = theme;
        this.fmt = new CurrencyFormatter(theme.rupee());
        this.fast = fast;
        this.animate = !fast && interactive;
        this.clearEnabled = theme.isColor() && interactive;
    }

    // ------------------------------------------------------------- utilities

    /** Formats an amount like the UI does (Indian grouping). */
    public String money(BigDecimal v) {
        return fmt.format(v);
    }

    /** Terminal display width: ANSI ignored, emoji counted as two columns. */
    static int displayWidth(String s) {
        String plain = Theme.stripAnsi(s);
        int w = 0;
        for (int i = 0; i < plain.length(); ) {
            int cp = plain.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 0xFE0F || cp == 0x200D) {
                continue;
            }
            w += cp >= 0x1F300 ? 2 : 1;
        }
        return w;
    }

    static String padRight(String s, int w) {
        return s + " ".repeat(Math.max(0, w - displayWidth(s)));
    }

    static String padLeft(String s, int w) {
        return " ".repeat(Math.max(0, w - displayWidth(s))) + s;
    }

    static String center(String s, int w) {
        int d = Math.max(0, w - displayWidth(s));
        return " ".repeat(d / 2) + s + " ".repeat(d - d / 2);
    }

    /** Left text and right text on one line of the given width. */
    static String row(String left, String right, int w) {
        int gap = Math.max(1, w - displayWidth(left) - displayWidth(right));
        return left + " ".repeat(gap) + right;
    }

    /** "Good morning" / "Good afternoon" / "Good evening". */
    public static String greetingFor(LocalTime time) {
        int h = time.getHour();
        return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    private static String firstName(String name) {
        return name.trim().split("\\s+")[0];
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 3)) + "...";
    }

    private void println(String s) {
        out.println(s);
    }

    /** Prints an empty line. */
    public void blank() {
        out.println();
    }

    /** Clears the screen (only on a colour-capable real terminal). */
    public void clear() {
        if (clearEnabled) {
            out.print(t.clearScreen());
            out.flush();
        }
    }

    // ------------------------------------------------------------ primitives

    private void panel(BoxStyle style, Function<String, String> tone, int width,
                       String title, List<String> lines) {
        Box b = t.box(style);
        int inner = width - 2;
        println(tone.apply(b.tl + b.h.repeat(inner) + b.tr));
        if (title != null) {
            println(tone.apply(b.v) + " " + padRight(t.bold(tone.apply(center(title, width - 4))), width - 4)
                    + " " + tone.apply(b.v));
            println(tone.apply(b.lt + b.h.repeat(inner) + b.rt));
        }
        for (String line : lines) {
            if (SEP.equals(line)) {
                println(tone.apply(b.lt + b.h.repeat(inner) + b.rt));
            } else {
                println(tone.apply(b.v) + " " + padRight(line, width - 4) + " " + tone.apply(b.v));
            }
        }
        println(tone.apply(b.bl + b.h.repeat(inner) + b.br));
    }

    /** Prompt with a coloured arrow, label and dim hint. */
    public void prompt(String label, String hint) {
        String h = hint == null || hint.isEmpty() ? "" : " " + t.dim("(" + hint + ")");
        out.print(t.yellow(t.prompt()) + " " + label + h + ": ");
        out.flush();
    }

    /** Prompt used for "Press Enter" pauses. */
    public void promptEnter(String message) {
        out.print(t.yellow(t.prompt()) + " " + t.dim(message));
        out.flush();
    }

    public void success(String m) { println(t.green(t.ok() + " " + m)); }
    public void error(String m) { println(t.red(t.err() + " " + m)); }
    public void warn(String m) { println(t.yellow(t.warn() + " " + m)); }
    public void info(String m) { println(t.cyan(m)); }
    public void hint(String m) { println(t.dim(m)); }

    // --------------------------------------------------------------- screens

    /** Banner, bank name, date/time, instructions and the start-up spinner. */
    public void showWelcome(String bankName) {
        clear();
        blank();
        for (String line : BANNER) {
            println(t.bold(t.cyan(center(line, WIDTH))));
        }
        blank();
        println(t.bold(center(bankName, WIDTH)));
        println(t.dim(center(LocalDateTime.now().format(LONG_DT), WIDTH)));
        println(t.cyan(center(t.box(BoxStyle.SINGLE).h.repeat(40), WIDTH)));
        println(center("Welcome! Please log in with your User ID and PIN.", WIDTH));
        blank();
        spinner("Initializing secure session...");
        blank();
    }

    /** Progress bar of about 1.2 s; skipped with --fast, static line when not on a terminal. */
    public void spinner(String message) {
        if (fast) {
            return;
        }
        if (!animate) {
            println(t.green(t.ok() + " " + message + " done"));
            return;
        }
        int steps = 12;
        try {
            for (int i = 1; i <= steps; i++) {
                String bar = t.barFull().repeat(i) + t.barEmpty().repeat(steps - i);
                out.print("\r  " + t.cyan(bar) + " " + padLeft(i * 100 / steps + "%", 4) + "  " + message);
                out.flush();
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        out.print("\r" + " ".repeat(WIDTH) + "\r");
        println(t.green(t.ok() + " " + message + " done"));
    }

    /** Login panel and an optional visible-PIN notice. */
    public void showLoginHeader(boolean maskingAvailable) {
        List<String> lines = new ArrayList<>();
        lines.add(center("Enter your User ID and 4-digit PIN", WIDTH - 4));
        panel(BoxStyle.SINGLE, t::cyan, WIDTH, "SECURE LOGIN", lines);
        if (!maskingAvailable) {
            warn("Note: this console cannot hide input, so your PIN will be visible.");
        }
        blank();
    }

    /** Red failure message and an attempts indicator such as "Attempts left: ●●○". */
    public void showLoginFailure(int remaining, int max) {
        error("Incorrect User ID or PIN.");
        println(t.dim("  Attempts left: ") + t.yellow(t.dotFull().repeat(remaining) + t.dotEmpty().repeat(max - remaining))
                + t.dim(" (" + remaining + " of " + max + ")"));
        blank();
    }

    public void showLoginSuccess(Account a) {
        blank();
        success("Login successful. Welcome, " + firstName(a.getHolderName()) + "!");
    }

    /** Final lock screen after too many failed attempts. */
    public void showLockScreen() {
        blank();
        List<String> lines = new ArrayList<>();
        lines.add(center(t.red(t.err() + " Access Denied: too many incorrect attempts"), WIDTH - 4));
        lines.add(SEP);
        lines.add(center("For your security this terminal is now locked.", WIDTH - 4));
        lines.add(center("Please contact your bank for assistance.", WIDTH - 4));
        panel(BoxStyle.DOUBLE, t::red, WIDTH, "ACCESS DENIED", lines);
    }

    /** Main menu card. */
    public void showMenu(Account a) {
        clear();
        LocalDateTime now = LocalDateTime.now();
        int cw = WIDTH - 4;
        List<String> l = new ArrayList<>();
        l.add(t.bold(greetingFor(now.toLocalTime()) + ", " + firstName(a.getHolderName())));
        l.add(t.dim(now.format(MENU_DT)));
        l.add(SEP);
        l.add(row(t.dim("Account"), a.getMaskedId(), cw));
        l.add(row(t.dim("Available Balance"), t.boldWhite(fmt.format(a.getBalance())), cw));
        l.add(SEP);
        for (int i = 0; i < MENU_LABELS.length; i++) {
            l.add("  " + t.key(String.valueOf(i + 1)) + "   " + t.icon(i) + "  " + MENU_LABELS[i]);
        }
        l.add(SEP);
        l.add(row(t.dim("Enter 1-5"), t.dim(t.lock() + "Session secured"), cw));
        panel(BoxStyle.DOUBLE, t::cyan, WIDTH, "MAIN MENU", l);
        blank();
    }

    private int amountWidth() {
        return 12 + t.rupee().length();
    }

    private String timeFormatPattern() {
        return t.isAscii() ? "dd/MM HH:mm" : "dd/MM HH:mm:ss";
    }

    /** Aligned history table with totals footer. */
    public void showHistory(Account a) {
        clear();
        List<Transaction> txns = a.getTransactions();
        int idW = 10;
        int timeW = timeFormatPattern().length();
        int typeW = 12;
        int amtW = amountWidth();
        int[] w = {idW, timeW, typeW, amtW, amtW};
        int tableW = 4 + 4 * 3 + idW + timeW + typeW + 2 * amtW;
        int cw = tableW - 4;

        List<String> head = new ArrayList<>();
        head.add(row(t.dim("Account"), a.getMaskedId() + "  " + a.getHolderName(), cw));
        head.add(row(t.dim("Current balance"), t.boldWhite(fmt.format(a.getBalance())), cw));
        panel(BoxStyle.DOUBLE, t::cyan, tableW, "TRANSACTION HISTORY", head);

        if (txns.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("");
            empty.add(center("No transactions yet in this session.", WIDTH - 4));
            empty.add(center(t.dim("Make a deposit or withdrawal and it will appear here."), WIDTH - 4));
            empty.add("");
            panel(BoxStyle.SINGLE, t::cyan, WIDTH, null, empty);
            return;
        }

        Box b = t.box(BoxStyle.SINGLE);
        String v = t.cyan(b.v);
        String[] allTee = {b.tt, b.tt, b.tt, b.tt};
        println(hline(b, w, b.tl, allTee, b.tr));
        println(tableRow(v, w, new String[]{t.bold("Txn ID"), t.bold("Date/Time"), t.bold("Type"),
                padLeft(t.bold("Amount"), amtW), padLeft(t.bold("Balance"), amtW)}));
        String[] cross = {b.x, b.x, b.x, b.x};
        println(hline(b, w, b.lt, cross, b.rt));

        int spanW = timeW + typeW + 2 * amtW + 9;
        DateTimeFormatter tf = DateTimeFormatter.ofPattern(timeFormatPattern(), Locale.ENGLISH);
        for (int i = 0; i < txns.size(); i++) {
            Transaction tx = txns.get(i);
            if (i > 0) {
                println(hline(b, w, b.lt, new String[]{b.x, b.tt, b.tt, b.tt}, b.rt));
            }
            boolean credit = tx.getType().isCredit();
            String amount = credit ? t.green("+" + fmt.format(tx.getAmount()))
                                   : t.red("-" + fmt.format(tx.getAmount()));
            String rowLine = tableRow(v, w, new String[]{tx.getId(), tx.getTimestamp().format(tf),
                    tx.getType().getLabel(), padLeft(amount, amtW), padLeft(fmt.format(tx.getBalanceAfter()), amtW)});
            String detail = v + " " + padRight("", idW) + " " + v + " "
                    + padRight(t.dim(t.detailArrow() + " " + shorten(tx.getDescription(), spanW - 3)), spanW) + " " + v;
            if (i % 2 == 1) {
                rowLine = t.zebra(rowLine);
                detail = t.zebra(detail);
            }
            println(rowLine);
            println(detail);
        }
        String[] flat = {b.h, b.h, b.h, b.h};
        println(hline(b, w, b.lt, new String[]{b.bt, b.h, b.h, b.h}, b.rt));

        SessionSummary s = SessionSummary.of(txns);
        println(v + " " + padRight(row("Deposited  " + t.green(fmt.format(s.deposited())),
                "Withdrawn  " + t.red(fmt.format(s.withdrawn())), cw), cw) + " " + v);
        println(v + " " + padRight(row("Transferred out  " + t.red(fmt.format(s.transferredOut())),
                "Transferred in  " + t.green(fmt.format(s.transferredIn())), cw), cw) + " " + v);
        BigDecimal net = s.netChange();
        String netText = net.signum() >= 0 ? t.green("+" + fmt.format(net)) : t.red(fmt.format(net));
        println(v + " " + padRight(row(t.bold(s.count() + " transaction(s)"), "Net change  " + netText, cw), cw) + " " + v);
        println(hline(b, w, b.bl, flat, b.br));
        blank();
    }

    private String hline(Box b, int[] w, String left, String[] mids, String right) {
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < w.length; i++) {
            sb.append(b.h.repeat(w[i] + 2));
            if (i < w.length - 1) {
                sb.append(mids[i]);
            }
        }
        return t.cyan(sb.append(right).toString());
    }

    private String tableRow(String v, int[] w, String[] cells) {
        StringBuilder sb = new StringBuilder(v);
        for (int i = 0; i < cells.length; i++) {
            sb.append(' ').append(padRight(cells[i], w[i])).append(' ').append(v);
        }
        return sb.toString();
    }

    /** Withdraw screen with quick-select amounts. */
    public void showWithdrawScreen(Account a, BigDecimal[] quick) {
        clear();
        int cw = WIDTH - 4;
        List<String> l = new ArrayList<>();
        l.add(row(t.dim("Available balance"), t.boldWhite(fmt.format(a.getBalance())), cw));
        l.add(SEP);
        for (int i = 0; i < quick.length; i++) {
            l.add("  " + t.key(String.valueOf(i + 1)) + "   " + fmt.format(quick[i]));
        }
        l.add("  " + t.key(String.valueOf(quick.length + 1)) + "   Other amount");
        l.add("  " + t.key("0") + t.dim("   Cancel (or type b)"));
        l.add(SEP);
        l.add(t.dim("Multiples of " + fmt.format(Account.WITHDRAW_MULTIPLE).replace(".00", "")
                + ", max " + fmt.format(Account.MAX_TXN_AMOUNT) + " per transaction"));
        panel(BoxStyle.DOUBLE, t::cyan, WIDTH, "WITHDRAW CASH", l);
        blank();
    }

    public void showDepositScreen(Account a) {
        clear();
        List<String> l = new ArrayList<>();
        l.add(row(t.dim("Current balance"), t.boldWhite(fmt.format(a.getBalance())), WIDTH - 4));
        l.add(t.dim("Max " + fmt.format(Account.MAX_TXN_AMOUNT) + " per transaction"));
        panel(BoxStyle.DOUBLE, t::cyan, WIDTH, "DEPOSIT CASH", l);
        blank();
    }

    public void showTransferScreen(Account a) {
        clear();
        List<String> l = new ArrayList<>();
        l.add(row(t.dim("Available balance"), t.boldWhite(fmt.format(a.getBalance())), WIDTH - 4));
        l.add(t.dim("Max " + fmt.format(Account.MAX_TXN_AMOUNT) + " per transaction"));
        panel(BoxStyle.DOUBLE, t::cyan, WIDTH, "FUND TRANSFER", l);
        blank();
    }

    /** "You are about to ..." summary before a withdrawal or transfer. */
    public void showConfirmation(String headline, List<String[]> rows) {
        blank();
        List<String> l = new ArrayList<>();
        l.add(t.yellow(t.warn() + " " + headline));
        l.add(SEP);
        for (String[] r : rows) {
            l.add(row(t.dim(r[0]), r[1], WIDTH - 4));
        }
        panel(BoxStyle.SINGLE, t::yellow, WIDTH, "CONFIRM TRANSACTION", l);
    }

    /** Dashed receipt panel shown after every successful transaction. */
    public void showReceipt(Transaction tx, Account a) {
        blank();
        int cw = RECEIPT_WIDTH - 4;
        List<String> l = new ArrayList<>();
        l.add(center(t.green(t.ok() + " Transaction Successful"), cw));
        l.add(SEP);
        l.add(row(t.dim("Txn ID"), tx.getId(), cw));
        l.add(row(t.dim("Date/Time"), tx.getTimestamp().format(RECEIPT_DT), cw));
        l.add(row(t.dim("Type"), tx.getType().getLabel(), cw));
        l.add(row(t.dim("Amount"), t.boldWhite(fmt.format(tx.getAmount())), cw));
        l.add(row(t.dim("New balance"), t.boldWhite(fmt.format(tx.getBalanceAfter())), cw));
        l.add(row(t.dim("Account"), a.getMaskedId(), cw));
        l.add(SEP);
        l.add(center("Thank you for banking with us", cw));
        panel(BoxStyle.DASHED, t::cyan, RECEIPT_WIDTH, "RECEIPT", l);
        blank();
    }

    /** Parses an InvalidAmountException into polite guidance. */
    public void showAmountError(InvalidAmountException e) {
        switch (e.getReason()) {
            case NOT_POSITIVE -> error("Please enter an amount greater than zero.");
            case TOO_MANY_DECIMALS -> error("Amounts can have at most 2 decimal places.");
            case EXCEEDS_LIMIT -> error("The maximum per transaction is " + fmt.format(Account.MAX_TXN_AMOUNT) + ".");
            case NOT_MULTIPLE -> error("Please enter an amount in multiples of "
                    + fmt.format(Account.WITHDRAW_MULTIPLE).replace(".00", "") + ".");
        }
    }

    public void showInsufficientFunds(InsufficientFundsException e) {
        blank();
        error("Insufficient Funds");
        hint("  Requested " + fmt.format(e.getRequested()) + ", available " + fmt.format(e.getAvailable()) + ".");
        blank();
    }

    /** Farewell with session summary. */
    public void showGoodbye(Account a, SessionSummary s) {
        clear();
        int cw = WIDTH - 4;
        BigDecimal net = s.netChange();
        String netText = net.signum() >= 0 ? t.green("+" + fmt.format(net)) : t.red(fmt.format(net));
        List<String> l = new ArrayList<>();
        l.add(center("Thank you for using our ATM, " + firstName(a.getHolderName()) + "!", cw));
        l.add(SEP);
        l.add(row(t.dim("Transactions this session"), String.valueOf(s.count()), cw));
        l.add(row(t.dim("Net change in balance"), netText, cw));
        l.add(row(t.dim("Closing balance"), t.boldWhite(fmt.format(a.getBalance())), cw));
        l.add(SEP);
        l.add(center(t.yellow("Please remember to take your card." + t.card()), cw));
        l.add(center(t.dim("Have a great day!"), cw));
        panel(BoxStyle.DOUBLE, t::cyan, WIDTH, "GOODBYE", l);
        blank();
    }

    /** Shown when input ends unexpectedly (EOF). */
    public void showInputClosed() {
        blank();
        warn("Input closed. Ending the session safely. Goodbye!");
    }

    /** Shown for unknown command-line flags. */
    public void showUsage() {
        println("Usage: java -cp <classes> com.atm.Main [options]");
        println("  --no-color   disable ANSI colours and screen clearing");
        println("  --color      force ANSI colours (e.g. in an IDE console that supports them)");
        println("  --ascii      use + - | and Rs. instead of Unicode symbols");
        println("  --fast       skip the start-up animation");
        println("  --relogin    return to the login screen after Quit (kiosk mode)");
        println("  --help       show this help");
    }
}
