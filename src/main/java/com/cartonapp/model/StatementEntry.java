package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One invoice or active receipt movement in a customer's account statement. */
public record StatementEntry(LocalDate date, String description, String invoiceNumber,
        BigDecimal debit, BigDecimal credit, BigDecimal balance) {
    public StatementEntry withBalance(BigDecimal value) {
        return new StatementEntry(date, description, invoiceNumber, debit, credit, value);
    }
}
