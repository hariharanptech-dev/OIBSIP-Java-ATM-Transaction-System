package com.atm;

import java.math.BigDecimal;
import java.util.List;

/** Totals computed from a list of transactions. */
public record SessionSummary(int count, BigDecimal deposited, BigDecimal withdrawn,
                             BigDecimal transferredOut, BigDecimal transferredIn) {

    /** Builds a summary from the given transactions. */
    public static SessionSummary of(List<Transaction> transactions) {
        BigDecimal dep = Money.ZERO;
        BigDecimal wd = Money.ZERO;
        BigDecimal out = Money.ZERO;
        BigDecimal in = Money.ZERO;
        for (Transaction t : transactions) {
            switch (t.getType()) {
                case DEPOSIT -> dep = dep.add(t.getAmount());
                case WITHDRAWAL -> wd = wd.add(t.getAmount());
                case TRANSFER_OUT -> out = out.add(t.getAmount());
                case TRANSFER_IN -> in = in.add(t.getAmount());
            }
        }
        return new SessionSummary(transactions.size(), dep, wd, out, in);
    }

    /** Credits minus debits. */
    public BigDecimal netChange() {
        return deposited.add(transferredIn).subtract(withdrawn).subtract(transferredOut);
    }
}
