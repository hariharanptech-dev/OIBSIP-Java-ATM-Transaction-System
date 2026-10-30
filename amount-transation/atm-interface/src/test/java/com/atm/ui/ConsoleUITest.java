package com.atm.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ConsoleUITest {
    @Test
    void greetingDependsOnTimeOfDay() {
        assertEquals("Good morning", ConsoleUI.greetingFor(LocalTime.of(6, 0)));
        assertEquals("Good morning", ConsoleUI.greetingFor(LocalTime.of(11, 59)));
        assertEquals("Good afternoon", ConsoleUI.greetingFor(LocalTime.of(12, 0)));
        assertEquals("Good evening", ConsoleUI.greetingFor(LocalTime.of(17, 0)));
        assertEquals("Good evening", ConsoleUI.greetingFor(LocalTime.of(23, 30)));
    }

    @Test
    void displayWidthIgnoresAnsiAndCountsEmojiAsTwo() {
        Theme color = new Theme(true, false);
        assertEquals(5, ConsoleUI.displayWidth(color.red("hello")));
        assertEquals(2, ConsoleUI.displayWidth("\uD83D\uDCDC"));
        assertEquals("  ab", ConsoleUI.padLeft("ab", 4));
        assertEquals("ab  ", ConsoleUI.padRight("ab", 4));
    }

    @Test
    void themeEmitsNoEscapeCodesWhenColourIsOff() {
        Theme plain = new Theme(false, true);
        assertEquals("x", plain.red("x"));
        assertEquals("", plain.clearScreen());
        assertFalse(plain.zebra("row").contains("\u001B"));
        assertTrue(new Theme(true, false).red("x").startsWith("\u001B["));
    }

    @Test
    void asciiThemeFallsBack() {
        Theme ascii = new Theme(false, true);
        assertEquals("Rs.", ascii.rupee());
        assertEquals("+", ascii.box(Theme.BoxStyle.DOUBLE).tl);
        assertEquals("[W]", ascii.icon(1));
    }

    @Test
    void groupsIndianDigits() {
        assertEquals("1,25,000", CurrencyFormatter.groupIndian("125000"));
    }
}
