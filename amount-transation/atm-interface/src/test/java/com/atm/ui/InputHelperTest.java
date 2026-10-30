package com.atm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InputHelperTest {
    private static String errorFor(String raw) {
        return assertThrows(IllegalArgumentException.class, () -> InputHelper.parseAmount(raw)).getMessage();
    }

    @Test
    void parsesValidAmounts() {
        assertEquals(new BigDecimal("1500.00"), InputHelper.parseAmount("1500"));
        assertEquals(new BigDecimal("1500.50"), InputHelper.parseAmount("1,500.5"));
        assertEquals(new BigDecimal("250.00"), InputHelper.parseAmount("\u20B9250"));
        assertEquals(new BigDecimal("250.00"), InputHelper.parseAmount("Rs. 250"));
        assertEquals(new BigDecimal("10.10"), InputHelper.parseAmount("10.100"));
    }

    @Test
    void rejectsBadAmountsWithSpecificMessages() {
        assertTrue(errorFor("").contains("enter an amount"));
        assertTrue(errorFor("abc").contains("numbers only"));
        assertTrue(errorFor("12abc").contains("numbers only"));
        assertTrue(errorFor("1e5").contains("numbers only"));
        assertTrue(errorFor("-50").contains("negative"));
        assertTrue(errorFor("0.00").contains("greater than zero"));
        assertTrue(errorFor("1.234").contains("2 decimal places"));
        assertTrue(errorFor("99999999999").contains("too large"));
    }
}
