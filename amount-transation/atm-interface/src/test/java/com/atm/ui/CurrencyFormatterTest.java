package com.atm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CurrencyFormatterTest {
    private final CurrencyFormatter rupee = new CurrencyFormatter("\u20B9");

    @Test
    void usesIndianGrouping() {
        assertEquals("\u20B90.00", rupee.format(new BigDecimal("0")));
        assertEquals("\u20B9999.00", rupee.format(new BigDecimal("999")));
        assertEquals("\u20B91,000.00", rupee.format(new BigDecimal("1000")));
        assertEquals("\u20B999,999.99", rupee.format(new BigDecimal("99999.99")));
        assertEquals("\u20B91,00,000.00", rupee.format(new BigDecimal("100000")));
        assertEquals("\u20B91,25,000.50", rupee.format(new BigDecimal("125000.50")));
        assertEquals("\u20B91,23,45,678.90", rupee.format(new BigDecimal("12345678.9")));
    }

    @Test
    void asciiFallbackAndNegatives() {
        CurrencyFormatter rs = new CurrencyFormatter("Rs.");
        assertEquals("Rs.75,250.50", rs.format(new BigDecimal("75250.5")));
        assertEquals("-Rs.1,500.00", rs.format(new BigDecimal("-1500")));
    }

    @Test
    void roundsHalfUp() {
        assertEquals("\u20B90.01", rupee.format(new BigDecimal("0.005")));
    }
}
