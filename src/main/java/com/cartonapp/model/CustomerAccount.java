package com.cartonapp.model;

import java.math.BigDecimal;

/**
 * Query-time totals of one customer's account: opening debt, posted invoices (after line and
 * additional discounts) and active receipts. Nothing here is stored.
 */
public record CustomerAccount(BigDecimal openingBalance, BigDecimal invoicesTotal, BigDecimal collected) {
    /** المديونية = الرصيد الافتتاحي + إجمالي الفواتير − المحصّل. */
    public BigDecimal balance() {
        return openingBalance.add(invoicesTotal).subtract(collected);
    }
}
