package com.cartonapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Calculated invoice row; total and paid are derived from lines and receipts. */
public record InvoiceSummary(long id, String number, String customerCode, String customerName,
        LocalDate date, String status, BigDecimal total, BigDecimal paid) {
    /** الباقي: what is still owed on this invoice; an overpayment shows as zero, never negative. */
    public BigDecimal remaining() { return total.subtract(paid).max(BigDecimal.ZERO); }
}
