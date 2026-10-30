package com.atm.ui;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * The single home of every ANSI escape, colour, symbol and box-drawing character.
 * When colour is off no escape code is ever returned; when ASCII mode is on every
 * non-ASCII symbol is replaced by a plain equivalent.
 * (All non-ASCII characters are written as \\u escapes so plain javac works on any locale.)
 */
public final class Theme {
    // ---- ANSI codes (only defined here) ----
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[97m";
    private static final String GRAY = "\u001B[90m";
    private static final String ZEBRA_BG = "\u001B[48;5;236m";
    private static final String CLEAR = "\u001B[H\u001B[2J\u001B[3J";
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[;\\d]*[A-Za-z]");

    /** Border style families. */
    public enum BoxStyle { DOUBLE, SINGLE, DASHED }

    /** Set of box-drawing characters (junctions included for tables). */
    public static final class Box {
        public final String tl, tr, bl, br, h, v, lt, rt, tt, bt, x;

        Box(String tl, String tr, String bl, String br, String h, String v,
            String lt, String rt, String tt, String bt, String x) {
            this.tl = tl; this.tr = tr; this.bl = bl; this.br = br; this.h = h; this.v = v;
            this.lt = lt; this.rt = rt; this.tt = tt; this.bt = bt; this.x = x;
        }
    }

    private static final Box DOUBLE = new Box("\u2554", "\u2557", "\u255A", "\u255D", "\u2550", "\u2551",
            "\u2560", "\u2563", "\u2566", "\u2569", "\u256C");
    private static final Box SINGLE = new Box("\u250C", "\u2510", "\u2514", "\u2518", "\u2500", "\u2502",
            "\u251C", "\u2524", "\u252C", "\u2534", "\u253C");
    private static final Box DASHED = new Box("\u250C", "\u2510", "\u2514", "\u2518", "\u2504", "\u2506",
            "\u251C", "\u2524", "\u252C", "\u2534", "\u253C");
    private static final Box ASCII_DOUBLE = new Box("+", "+", "+", "+", "=", "|", "+", "+", "+", "+", "+");
    private static final Box ASCII_SINGLE = new Box("+", "+", "+", "+", "-", "|", "+", "+", "+", "+", "+");

    private static final String[] ICONS = {
        "\uD83D\uDCDC", "\uD83D\uDCB8", "\uD83D\uDCB0", "\uD83D\uDD01", "\uD83D\uDEAA"};
    private static final String[] ASCII_ICONS = {"[H]", "[W]", "[D]", "[T]", "[Q]"};

    private final boolean color;
    private final boolean ascii;

    public Theme(boolean color, boolean ascii) {
        this.color = color;
        this.ascii = ascii;
    }

    // ------------------------------------------------------------ detection

    /**
     * Decides the theme from flags and the environment.
     *
     * @param noColor     --no-color given
     * @param forceColor  --color given (overrides detection, e.g. for IDE consoles that do support ANSI)
     * @param ascii       --ascii given
     * @param interactive true when stdin/stdout are attached to a real terminal
     */
    public static Theme detect(boolean noColor, boolean forceColor, boolean ascii, boolean interactive) {
        boolean color;
        if (noColor) {
            color = false;
        } else if (forceColor) {
            color = true;
        } else {
            color = interactive && ansiLikelySupported();
        }
        return new Theme(color, ascii);
    }

    private static boolean ansiLikelySupported() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        String term = System.getenv("TERM");
        if ("dumb".equals(term)) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("WT_SESSION") != null || System.getenv("ANSICON") != null
                    || "ON".equals(System.getenv("ConEmuANSI")) || term != null
                    || System.getenv("TERM_PROGRAM") != null;
        }
        return true;
    }

    /** @return true when a real terminal (not a pipe, file or most IDE consoles) is attached */
    public static boolean isInteractiveTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            Method m = Console.class.getMethod("isTerminal"); // JDK 22+
            return (Boolean) m.invoke(console);
        } catch (ReflectiveOperationException e) {
            return true; // JDK 17-21: console() is non-null only for a real terminal
        }
    }

    // --------------------------------------------------------------- queries

    public boolean isColor() { return color; }
    public boolean isAscii() { return ascii; }

    /** Removes ANSI escapes (used for width measuring and in tests). */
    public static String stripAnsi(String s) {
        return ANSI.matcher(s).replaceAll("");
    }

    // ---------------------------------------------------------------- styles

    private String wrap(String code, String s) {
        return color ? code + s + RESET : s;
    }

    public String cyan(String s) { return wrap(CYAN, s); }
    public String green(String s) { return wrap(GREEN, s); }
    public String red(String s) { return wrap(RED, s); }
    public String yellow(String s) { return wrap(YELLOW, s); }
    public String dim(String s) { return wrap(GRAY, s); }
    public String bold(String s) { return wrap(BOLD, s); }
    public String boldWhite(String s) { return wrap(BOLD + WHITE, s); }
    /** Highlight for menu number keys. */
    public String key(String s) { return wrap(BOLD + CYAN, s); }
    /** Faint text (secondary rows). */
    public String faint(String s) { return wrap(DIM, s); }

    /** Subtle background band for alternating table rows. */
    public String zebra(String line) {
        return color ? ZEBRA_BG + line.replace(RESET, RESET + ZEBRA_BG) + RESET : line;
    }

    /** @return the clear-screen sequence (empty without colour) */
    public String clearScreen() { return color ? CLEAR : ""; }

    // --------------------------------------------------------------- symbols

    public String ok() { return ascii ? "[OK]" : "\u2714"; }
    public String err() { return ascii ? "[X]" : "\u2716"; }
    public String warn() { return ascii ? "[!]" : "\u26A0"; }
    public String prompt() { return ascii ? ">" : "\u25B6"; }
    public String dotFull() { return ascii ? "*" : "\u25CF"; }
    public String dotEmpty() { return ascii ? "o" : "\u25CB"; }
    public String detailArrow() { return ascii ? ">" : "\u21B3"; }
    public String sep() { return ascii ? " - " : " \u00B7 "; }
    public String lock() { return ascii ? "" : "\uD83D\uDD12 "; }
    public String card() { return ascii ? "" : " \uD83D\uDCB3"; }
    public String rupee() { return ascii ? "Rs." : "\u20B9"; }
    public String barFull() { return ascii ? "#" : "\u2588"; }
    public String barEmpty() { return ascii ? "-" : "\u2591"; }
    public String icon(int index) { return ascii ? ASCII_ICONS[index] : ICONS[index]; }

    /** @return the box characters for the requested style, honouring ASCII mode */
    public Box box(BoxStyle style) {
        if (ascii) {
            return style == BoxStyle.DOUBLE ? ASCII_DOUBLE : ASCII_SINGLE;
        }
        return switch (style) {
            case DOUBLE -> DOUBLE;
            case SINGLE -> SINGLE;
            case DASHED -> DASHED;
        };
    }
}
