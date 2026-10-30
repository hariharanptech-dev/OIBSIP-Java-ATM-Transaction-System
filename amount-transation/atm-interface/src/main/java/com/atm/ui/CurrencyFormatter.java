package com.atm.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formats money with Indian digit grouping (1,25,000.50) and a rupee symbol or "Rs.". */
public final class CurrencyFormatter {
    private final String symbol;

    public CurrencyFormatter(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /** @return e.g. {@code ₹1,25,000.50}; negative values get a leading minus */
    public String format(BigDecimal value) {
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        String plain = v.abs().toPlainString();
        int dot = plain.indexOf('.');
        String integer = plain.substring(0, dot);
        String fraction = plain.substring(dot);
        return (v.signum() < 0 ? "-" : "") + symbol + groupIndian(integer) + fraction;
    }

    /** Groups digits as ...,xx,xx,xxx (last three, then pairs). */
    static String groupIndian(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder sb = new StringBuilder(digits.substring(digits.length() - 3));
        for (int i = rest.length(); i > 0; i -= 2) {
            sb.insert(0, rest.substring(Math.max(0, i - 2), i) + ",");
        }
        return sb.toString();
    }
}
