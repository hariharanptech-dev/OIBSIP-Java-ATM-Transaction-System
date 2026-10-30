package com.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helpers that keep every monetary value at scale 2 with HALF_UP rounding. */
public final class Money {
    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

    private Money() {
    }

    /** Parses text such as "1250.5" into a scale-2 BigDecimal. */
    public static BigDecimal of(String text) {
        return normalize(new BigDecimal(text));
    }

    /** Forces scale 2 using HALF_UP rounding. */
    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** @return true when the value carries real precision beyond 2 decimals */
    public static boolean hasMoreThanTwoDecimals(BigDecimal value) {
        return value.signum() != 0 && value.stripTrailingZeros().scale() > SCALE;
    }
}
