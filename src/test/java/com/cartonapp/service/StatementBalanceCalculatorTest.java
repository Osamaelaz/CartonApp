package com.cartonapp.service;

import com.cartonapp.model.StatementEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Lightweight dependency-free regression test for running statement balances. */
public final class StatementBalanceCalculatorTest {
    private StatementBalanceCalculatorTest() {
    }

    public static void main(String[] args) {
        StatementEntry invoice = new StatementEntry(LocalDate.of(2026, 5, 1),
                "فاتورة مبيعات مرحلة", "INV-1", new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO);
        StatementEntry receipt = new StatementEntry(LocalDate.of(2026, 5, 2),
                "تحصيل — نقدي", "INV-1", BigDecimal.ZERO,
                new BigDecimal("35.00"), BigDecimal.ZERO);

        List<StatementEntry> result = StatementBalanceCalculator.withBalances(List.of(invoice, receipt));
        require(result.get(0).balance().compareTo(new BigDecimal("100.00")) == 0,
                "invoice balance");
        require(result.get(1).balance().compareTo(new BigDecimal("65.00")) == 0,
                "receipt balance");
    }

    private static void require(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed: " + name);
        }
    }
}
