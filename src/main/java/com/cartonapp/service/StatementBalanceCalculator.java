package com.cartonapp.service;

import com.cartonapp.model.StatementEntry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Calculates running customer statement balances from debit and credit movements. */
final class StatementBalanceCalculator {
    private StatementBalanceCalculator() {
    }

    static List<StatementEntry> withBalances(List<StatementEntry> entries) {
        return withBalances(entries, BigDecimal.ZERO);
    }

    /** Running balances starting from {@code opening} (the balance before the first movement). */
    static List<StatementEntry> withBalances(List<StatementEntry> entries, BigDecimal opening) {
        List<StatementEntry> result = new ArrayList<>(entries.size());
        BigDecimal balance = opening == null ? BigDecimal.ZERO : opening;
        for (StatementEntry entry : entries) {
            balance = balance.add(entry.debit()).subtract(entry.credit());
            result.add(entry.withBalance(balance));
        }
        return result;
    }
}
